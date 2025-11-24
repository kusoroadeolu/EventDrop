# EventDrop – Architecture Documentation

## Quick Review
Got tired of sending files through Discord/WhatsApp just to get them on my phone and manually re-delete the files from there 
EventDrop is my solution - ephemeral file sharing with temporary rooms that automatically cleans itself. 
No accounts, no permanent storage, minimal friction.

## What it does
- Create or join rooms with 8-character codes
- Upload files (room owners only), download files (everyone)
- Real-time updates via Server-Sent Events
- Everything expires automatically - rooms, files, sessions

## Frontend (Web App)
Multiple page app that works on desktop and mobile. 
PWA Supported


**Role system:**
- **OWNER**: Upload/delete files, delete entire room, plus all occupant permissions
- **OCCUPANT**: Download files, receive live updates via SSE, leave room

**Session handling:**
- Uses cookies to persist session ID
- Sessions last 5 minutes, refresh on activity
- Lost session = refresh page and rejoin room

## Core Components

### Redis
Redis acts as the primary data store for rooms, occupants, and file metadata.

- **Why Redis?**  
  For an ephemeral service, using a relational DB would be overkill. Redis’s in-memory structures provide sub-millisecond latency for session checks and room lookups.  
  Its built-in **TTL (time-to-live)** support is crucial — every room, occupant, and file metadata entry expires automatically. This makes Redis a natural fit for the project’s short-lived data model.

- **Tradeoff:**  
  Redis gives you speed + TTL but sacrifices durability. That’s acceptable here since persistence isn’t a requirement.

---

### Event-Driven Communication
The system uses a hybrid event-driven approach:

- **RabbitMQ (asynchronous business logic):**  
  Handles async(fire and forget) events like room leaves, and expiry.  
  This keeps API calls snappy — complex logic (like cascading file deletions) runs in the background.

- **Spring ApplicationEventPublisher (in-process events):**  
  Used for lightweight, synchronous events like incrementing metrics or pushing updates to clients via SSE.  
  Queue based implementation when streaming events, 
  to prevent race conditions causing the SSE to disconnect when it tries to stream multiple events at once
  This avoids broker overhead when speed matters.

- **Why both?**  
  RabbitMQ ensures durability across services.  
  ApplicationEventPublisher gives instant responses inside the app.  
  Each was chosen for the scale and latency profile of the event it handles.

---

### Multi-Layered Cleanup
Data integrity and security are handled by **multiple layers of cleanup**:

1. **User-driven:** Manual deletion of files and rooms.
2. **Redis TTL:** Fast, automatic expiration at the DB level.
3. **Event-driven:** Expiry events trigger cascading cleanup of room-related data.
4. **Scheduled job:** A nightly sweep (2 AM) removes any orphaned Redis keys.
5. **Azure Blob lifecycle policy:** Final failsafe — any file older than 24 hours is deleted automatically at the storage level.

- **Why layered?**  
  Redis TTL alone is powerful but not foolproof. The layered approach ensures redundancy and reduces the risk of leaks or resource buildup.

---

## Challenges and Solutions

### Race Condition in Batch Uploads
- **Problem:** Simultaneous uploads could bypass the per-room file quota (30 files) because each upload checked the limit independently.
- **Solution:** Validate the batch as a whole before inserting. This atomic check enforces the quota fairly.

---

### RabbitMQ Message Failures
- **Problem:** Messages sometimes failed, causing unprocessed events to pile up.
- **Solution:**
    - Added a retry template with max 5 retries for transient failures.
    - Configured listener containers to handle retries gracefully.

## Backend (Spring Boot + Java 21)

### Data Storage

**Redis as primary database:**
```java
@RedisHash("room")
public class Room {
    @Id String roomCode;
    @TimeToLive long ttl;
}

@RedisHash("occupant") 
public class Occupant {
    @Id UUID sessionId;
    String roomCode;
    // TTL: 300 seconds, refreshed on requests
}

@RedisHash(value = "fileDrop", timeToLive = 86400)
public class FileDrop {
    @Id UUID fileId;
    String roomCode;
    // TTL: 1 day max to prevent data leaks so that the scheduled job can handle it
}
```

**Azure Blob Storage:**
- Actual file contents
- Signed URLs for downloads (no server proxy)
- 1-day lifecycle policy as final cleanup

### Event-Driven Cleanup
**RabbitMQ handles the coordination:**
- Room expiry triggers cascading cleanup
- 2-second delay between room deletion and file cleanup to prevent race conditions
- Failed deletions get marked as deleted, cleaned up when room expires

**Multi-layer cleanup strategy:**
1. User deletes stuff manually
2. Redis TTL expires naturally
3. Room expiry event cascades to all files/occupants
4. Scheduled job catches anything with ttl == -1 (but excludes Spring's index keys)
5. Azure lifecycle policy nukes everything after 1 day

### File Upload Quotas
**Current limits:**
- Max 30 files per room (prevents large SSE payloads from crashing clients)
- Max 2GB total size per room (prevents abuse as free storage service)
- Batch validation prevents concurrent uploads from exceeding limits and proper error handling
- Duplicate file prevention (your separate backend work)

### SSE Implementation
Sends full room state snapshots instead of diffs. Less efficient bandwidth-wise but way simpler to implement and handles reconnections cleanly. 
Tested with 20 files, runs smoothly. Will optimize if I ever get 1000+ users lol.

## Rate Limits
No rate limits applied for both the landing page and /rooms GET (to prevent sse streams from burning through the limits)
Default rate limits applied for downloading, joining rooms, leaving rooms,
Strict rate limits applied for all file related actions to prevent abuse

```json
{
  "roomName": "My Room",
  "roomCode": "ABCD1234",
  "fileDrops": [],
  "occupantCount": 3,
  "notification": "Bob joined the room",
  "expiresAt": "2025-09-14T22:06:00",
  "lastUpdated":  "2025-09-14T22:05:00",
  "isExpired" : "false"
}
```

### Scheduled Jobs
Implemented a scheduled job that runs every day at 2AM to clean up an orphaned keys that might've escaped Redis's eye 
This is essential to prevent any data leaks that will consume resources.

### Error Handling

**RabbitMQ listeners:**
- 2 queues 2 message types and 2 bindings for room leave, expiry events
- ListenerExecutionFailedException → requeue message/retry
- Generic exceptions → reject and don't requeue (prevents infinite loops)
- Room expiry failures just get logged (it's a one-time event anyway, these will later get cleaned up eventually)

**Partial failure handling:**
- Azure blob deletion fails? File gets marked as deleted, actual cleanup happens during room expiry
- If that fails too, Azure lifecycle policy handles it after 1 day
- Prioritized avoiding data leaks over perfect consistency

### User Experience
Room expiration is now handled gracefully. When a room expires, a boolean flag is set to true in the room's state. 
The frontend, upon receiving this update via SSE, redirects the user to the create/join room page, providing a seamless and intentional transition.
This  approach centralizes the expiration logic and avoids complex HTTP status code handling on the frontend. (Parsing http status codes with SSE emitters is a nightmare
Added PWAs for a mobile friendly alternative
Simple metrics for tracking rooms created, files uploaded and downloaded

## Testing
Wrote comprehensive unit tests that cover the most common user actions/flows

## Rate Limits
Implemented a sliding window rate limiter to prevent spam on file operations

## Key Design Decisions
**Why these choices:**
1. **Full SSE snapshots over diffs** - Simpler to implement, handles edge cases better, lightweight payload anyway
2. **Individual entity TTLs** - More reliable than complex nested key structures
3. **Event-driven cleanup** - Prevents race conditions between natural expiry and forced deletion
4. **Multi-layer safety nets** - Better to over-clean than leak user data
5. **Session-based auth** - No account friction, still provides role separation
6. **Batch quota validation** - Fixes race condition without maintaining separate Redis counters

Built this primarily for personal use but designed it to handle multiple users at a small scale. The architecture prioritizes reliability and simplicity over optimization - will add complexity only when usage demands it.