# EventDrop – Architecture Documentation

## Why I built this
Got tired of sending files through Discord/WhatsApp just to get them on my phone. 
EventDrop is my solution - ephemeral file sharing with temporary rooms that auto-delete everything. 
No accounts, no permanent storage, minimal friction.

## What it does

- Create or join rooms with 8-character codes
- Upload files (room owners only), download files (everyone)
- Real-time updates via Server-Sent Events
- Everything expires automatically - rooms, files, sessions

## Frontend (Web App)
Multiple page app that works on desktop and mobile. 
Planning to make it a PWA eventually.

**Role system:**
- **OWNER**: Upload/delete files, delete entire room, plus all occupant permissions
- **OCCUPANT**: Download files, receive live updates via SSE, leave room

**Session handling:**
- Uses cookies to persist session ID
- Sessions last 5 minutes, refresh on activity
- Lost session = refresh page and rejoin room

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
Had a race condition issue with batch uploads where multiple files could bypass quotas by checking limits simultaneously. 
Fixed it by validating the entire batch as one unit instead of individual files.

**Current limits:**
- Max 30 files per room (prevents large SSE payloads from crashing clients)
- Max 2GB total size per room (prevents abuse as free storage service)
- Batch validation prevents concurrent uploads from exceeding limits

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
- 3 queues 3 message types and 3 bindings for room join, leave, expiry events
- ListenerExecutionFailedException → requeue message
- Generic exceptions → reject and don't requeue (prevents infinite loops)
- Room expiry failures just get logged (it's a one-time event anyway, these will later get cleaned up eventually)

**Partial failure handling:**
- Azure blob deletion fails? File gets marked as deleted, actual cleanup happens during room expiry
- If that fails too, Azure lifecycle policy handles it after 1 day
- Prioritized avoiding data leaks over perfect consistency

### User Experience
Room expiration is now handled gracefully. When a room expires, a boolean flag is set to true in the room's state. 
The frontend, upon receiving this update via SSE, redirects the user to the create/join room page, providing a seamless and intentional transition.
This  approach centralizes the expiration logic and avoids complex HTTP status code handling on the frontend. (Parsing http status codes with SSE emitters is a nightmare)

## Testing
Wrote comprehensive unit tests that cover the most common user actions

## Key Design Decisions

**Why these choices:**

1. **Full SSE snapshots over diffs** - Simpler to implement, handles edge cases better, lightweight payload anyway
2. **Individual entity TTLs** - More reliable than complex nested key structures
3. **Event-driven cleanup** - Prevents race conditions between natural expiry and forced deletion
4. **Multi-layer safety nets** - Better to over-clean than leak user data
5. **Session-based auth** - No account friction, still provides role separation
6. **Batch quota validation** - Fixes race condition without maintaining separate Redis counters

## What's missing
- Not yet deployed on Azure(will do this next tomorrow)

## Future Optimizations(UX Based)
- PWA features (coming soon)
- Diff-based SSE (only if scale demands it)
- Backdoor keys for owners to reclaim rooms and password protected rooms(will do this who knows when lol)
Built this primarily for personal use but designed it to handle multiple users. The architecture prioritizes reliability and simplicity over optimization - will add complexity only when usage demands it.