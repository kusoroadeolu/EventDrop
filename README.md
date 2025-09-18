# EventDrop API

EventDrop is a lightweight, ephemeral file-sharing and collaborative room system. Users can create or join temporary rooms, upload/download files, and receive live updates about room state through SSE (Server-Sent Events).

This README documents the available REST endpoints, expected request/response formats, and usage notes.

---

## Base URLs

```
/rooms
/files
/metrics
```

All room-related endpoints are prefixed with `/rooms`. All file-related endpoints are prefixed with `/files`.

---

## Authentication

EventDrop uses **session-based authentication**:
NOTE: The backend already handles all role based authorization

- Clients must include a cookie containing the session ID for every authorized request:
  ```
  SESSION_ID : (SESSION_ID VALUE)
  Always replaces previous session IDs from cookies if the user enters a new room.
  ```
- The `SessionFilter` authenticates users against their session ID in Redis.
- Sessions automatically refresh on activity and expire after 5 minutes of inactivity.

---

## Rooms Endpoints

### 1. Create a Room

**POST** `/rooms/create`  
Creates a new room and automatically joins the creator as the owner.

**Request Body (`RoomCreateRequestDto`):**

```json
{
  "roomName": "My Room",
  "ttl": 15,
  "username": "Alice"
}
```

**Response (`RoomJoinResponseDto`):**

```json
{
  "roomName": "My Room",
  "username": "Alice",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "expiresAt": "2025-09-14T22:06:00"
}
```

**Status Codes:**
- `201 CREATED` – Room created successfully.
- `400 BAD REQUEST` – Validation error.

---

### 2. Join a Room

**POST** `/rooms/join`  
Join an existing room as an occupant.

**Request Body (`RoomJoinRequestDto`):**

```json
{
  "username": "Bob",
  "roomCode": "ABCD1234"
}
```

**Response (`RoomJoinResponseDto`):**

```json
{
  "roomName": "My Room",
  "username": "Bob",
  "sessionId": "550e8400-e29b-41d4-a716-446655440001",
  "expiresAt": "2025-09-14T22:06:00"
}
```

**Status Codes:**
- `200 OK` – Joined successfully.
- `404 NOT FOUND` – Room code does not exist.

---

### 3. Leave a Room

**DELETE** `/rooms/leave`  
**Authorization:** `OCCUPANT` or `OWNER`  
Leaves the current room.

**Response:** `200 OK`

---

### 4. Delete a Room

**DELETE** `/rooms/delete`  
**Authorization:** `OWNER`  
Deletes the room you own and removes all occupants.

**Response:** `204 NO CONTENT`

---

### 5. Stream Room State (SSE)

**GET** `/rooms/`  
**Authorization:** `OCCUPANT` or `OWNER`  
Connects to a room’s Server-Sent Event stream to receive live room updates (`RoomStateDto`):

**Example SSE payload:**

```json
{
  "roomName": "My Room",
  "roomCode": "ABCD1234",
  "fileDrops": [
    {
      "fileId": "file-123",
      "fileName": "example.txt",
      "fileSizeInBytes": 123456, 
      "uploadedAt": "2025-09-14T21:50:00"
    }
  ],
  "occupantCount": 3,
  "notification": "Bob has joined the room",
  "expiresAt": "2025-09-14T22:06:00",
  "lastUpdated": "2025-09-14T21:55:00"
}
```

**Notes:**
- SSE connections last until the room expires or the client disconnects.
- Notifications can be used to trigger toasts on the front end.

---

## Metrics Endpoints

### 1. Get Metrics
**GET** `/metrics`  
**Authorization:** `NONE`
Uploads a single file to the room.

**Request:** None

**Response (`SimpleMetricsDto`):**

```json
{
    "totalRoomsCreated": 3,
    "totalFilesUploaded": 1,
    "totalFilesDownloaded": 2
}
```

## Files Endpoints

### 1. Upload a File

**POST** `/files`  
**Authorization:** `OWNER`
Uploads a single file to the room.

**Request:** Multipart form-data with `file` key.

**Response (`FileDropResponseDto`):**

```json
{
  "fileId": "file-123",
  "fileName": "example.txt",
  "fileSizeInBytes": 123456,
  "uploadedAt": "2025-09-14T21:50:00"
}
```

**Status Codes:**
- `201 CREATED` – Upload successful.
- `400 BAD REQUEST` – Invalid file.

---

### 2. Upload Multiple Files

**POST** `/files/batch`  
**Authorization:** `OWNER`  
Uploads multiple files at once.

**Request:** Multipart form-data with multiple `file` keys.

**Response (`BatchUploadResult`):**

```json
{
  "successfulUploads": [],
  "failedUploads": []
}
```

**Status Codes:**
- `201 CREATED`

---

### 3. Download a File

**GET** `/files/{id}`  
**Authorization:** `OCCUPANT` or `OWNER`  
Returns a URL for file download. This URL should be redirected to the browser for file downloads (The frontend should handle this)

**Response (`FileDownloadResponseDto`):**

```json
{
  "downloadUrl": "https://..."
}
```

**Status Codes:**
- `302 FOUND` – Redirect to download URL.
- `404 NOT FOUND` – File not found.

---

### 4. Delete Files

**DELETE** `/files`  
**Authorization:** `OWNER`  
Deletes one or multiple files.

**Request Body:** List of file IDs (strings).

**Response (`BatchDeleteResult`):**

```json
{
  "deletedFiles": [],
  "failedDeletes": []
}
```

**Status Codes:**
- `204 NO CONTENT`
- `400 BAD REQUEST` – Invalid request.

---

## DTO Summary

- `RoomCreateRequestDto` – Info for creating a room.
- `RoomJoinRequestDto` – Info for joining a room.
- `RoomJoinResponseDto` – Response when joining or creating a room.
- `FileDropResponseDto` – File metadata.
- `FileDownloadResponseDto` – File download redirect.
- `BatchUploadResult` / `BatchDeleteResult` – Batch operations results.
- `RoomStateDto` – Aggregated room state streamed via SSE.

---

## Security
NOTE: All role based auth is handled by the backend
- `/rooms/leave` and `/rooms/` endpoints require roles: `OCCUPANT` or `OWNER`.
- `/rooms/delete` requires role: `OWNER`.

---

## Notes

- All room codes are 8-character strings.
- TTL is in minutes, after which rooms expire automatically.
- SSE updates are ephemeral.
- This system is designed for ephemeral file sharing; no permanent storage is assumed.

# Event Drop – Architecture Documentation (v4)

## Goal
Ephemeral, utility-style file sharing tool with minimal friction:
- Quick room creation (short-lived, no accounts)
- Self-destructing files via TTL
- Real-time updates across all clients
- Consistent behavior with proper cleanup

## 1. Client Architecture

**Universal Web App** (Desktop/Mobile/PWA-capable)
- Single codebase, role-based access control
- Creator vs participant permissions
- Real-time updates via Server-Sent Events (SSE)

**Core Capabilities:**
- Create/join rooms with generated codes
- Choose display names (unique per room)
- Upload/download files with progress tracking
- Real-time notifications (uploads, deletes, joins/leaves)
- Full room state snapshots via API

**Local Storage:**
- localStorage/sessionStorage only for session persistence
- Session ID and room join state
- No IndexedDB complexity

## 2. Backend Architecture

**Stack:** Java 21 + Spring Boot with Virtual Threads

### Data Storage

**Redis (Primary Store):**
Individual entities with their own TTL management:
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

@RedisHash("fileDrop")
public class FileDrop {
    @Id UUID fileId;
    String roomCode;
    @TimeToLive long ttlInSeconds;
}
```

**Azure Blob Storage:**
- Encrypted file contents
- Signed URLs for secure downloads
- 3-day lifecycle policy as cleanup safety net

### Event-Driven Architecture

**RabbitMQ Messaging:**
- Room expiry events trigger cascading cleanup
- File/occupant expiry events handle database cleanup
- Decoupled cleanup prevents race conditions

**Cleanup Strategy (Multi-layered):**
1. **Immediate**: User-initiated deletions
2. **TTL-based**: Redis expiration triggers cleanup events
3. **Room Expiry**: RabbitMQ events expire all room contents with 2-second delay
4. **Safety Net**: Scheduled job removes items with `ttl < -2`
5. **Final Cleanup**: Azure lifecycle policy (3 days)

### Session Management
- 5-minute TTL, refreshed on every API request
- Automatic cleanup for disconnected clients
- Session ID stored in localStorage for reconnection

## 3. Security Model

**No Traditional Auth:**
- Ephemeral session IDs (UUIDs) scoped per room
- Room code acts as access key
- Role-based permissions (creator/participant)

**File Security:**
- Signed download URLs (time-limited, single-use)
- Optional encryption with keys tied to Redis TTL
- Direct blob storage access via signed URLs

## 4. Key Workflows

### Room Lifecycle
1. **Creation**: Generate room code, store in Redis with TTL
2. **Expiry**: Redis TTL triggers cleanup chain:
    - Set 2-second TTL on all room files/occupants
    - Delete room metadata
    - RabbitMQ broadcasts room expiry event
    - Connected clients receive expiry notification

### File Operations
1. **Upload**:
    - Quota validation via Redis
    - Async upload to Azure Blob (virtual threads)
    - Store metadata in Redis with TTL
    - Broadcast upload event via SSE

2. **Download**:
    - Generate signed URL from Azure
    - Direct client-to-blob streaming

3. **Cleanup**:
    - Natural TTL expiry → Redis event → Database cleanup
    - Failed deletions → Mark as deleted, wait for room expiry
    - Scheduled job catches stragglers (`ttl <= 0`)

### Race Condition Prevention
- **SSE State Management**: Always send full room state snapshots, never diffs
- **Cleanup Delays**: 2-second buffer between room expiry and file cleanup
- **Event Ordering**: RabbitMQ ensures proper cleanup sequence
- **Idempotent Operations**: All cleanup operations handle missing entities gracefully

## 5. Implementation Patterns

**Async-First Design:**
- Virtual threads for I/O operations
- CompletableFuture for parallel processing
- Non-blocking event processing

**Resilient Cleanup:**
- Multiple cleanup layers prevent data leaks
- Graceful handling of partial failures
- Extensive logging for troubleshooting

**Stateless Design:**
- Session state in Redis only
- Client reconnection via stored session ID
- No server-side session management

## 6. Monitoring & Error Handling

**Cleanup Verification:**
- Event listeners with comprehensive logging
- Exception handling with fallback strategies
- Debug capabilities for troubleshooting expiry events

**Performance Considerations:**
- Redis key pattern optimization
- Parallel cleanup processing
- Efficient blob storage operations

## Key Architectural Decisions

1. **Individual Entity TTLs** over complex nested keys - simpler and more reliable
2. **Event-driven cleanup** prevents race conditions between natural and forced expiry
3. **Multi-layered safety nets** ensure no orphaned data
4. **Direct blob access** reduces server load and improves performance
5. **Unified client model** eliminates artificial PWA restrictions

This architecture prioritizes reliability, simplicity, and proper cleanup over complex optimizations.