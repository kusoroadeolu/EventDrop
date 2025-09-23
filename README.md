# EventDrop API Documentation

EventDrop is basically my solution to stop sending files through Discord/WhatsApp to get them on my phone. It's an ephemeral file sharing system where you create temporary rooms, share files, and everything auto-deletes after a set time.
Built this with Spring Boot, Redis, RabbitMQ, and Azure Blob Storage. 
Uses SSE for real-time updates.

## Architecture (Summary)
EventDrop is designed for **ephemeral file sharing** with automatic cleanup.  
Key choices:

- **Redis (with TTL):** Fast, in-memory store for rooms, occupants, and metadata.
- **Azure Blob Storage:** Files stored outside the app, auto-deleted by lifecycle policies.
- **Event-driven cleanup:** RabbitMQ for durable async tasks, Spring ApplicationEventPublisher for in-app events.
- **Multi-layer cleanup:** User deletes → Redis TTL → RabbitMQ expiry → nightly job → Azure policy.
- **SSE for real-time updates:** Full room snapshots keep clients simple and robust.
- **Quotas + rate limits:** Prevents abuse (30 files, 2GB max per room, strict upload limits).

👉 See [ARCHITECTURE.md](./ARCHITECTURE.md) for details.

## How it works
+ Create a room or join one with a room code. 
+ Upload files (only room owners can upload). 
+ Everyone in the room gets live updates when stuff happens. 
+ Room expires automatically and everything gets cleaned up.

## Base paths

```
/rooms   - room management
/files   - file operations  
/metrics - basic system stats
```

## Auth

Session-based auth using cookies. No account creation needed.

- Every request needs the SESSION_ID cookie
- Sessions last 5 minutes, refresh on activity
- Backend handles all the role checking (OWNER vs OCCUPANT)

## Room endpoints

### Create room
`POST /rooms/create`

```json
{
  "roomName": "My Room", 
  "ttl": 15,
  "username": "Alice"
}
```

Returns:
```json
{
  "roomName": "My Room",
  "username": "Alice", 
  "expiresAt": "2025-09-14T22:06:00"
}
```

### Join room
`POST /rooms/join`

```json
{
  "username": "Bob",
  "roomCode": "ABCD1234" 
}
```

Same response format as create.

### Leave room
`DELETE /rooms/leave`
Requires: OCCUPANT or OWNER role

### Delete room
`DELETE /rooms/delete`
Requires: OWNER role
Nukes the whole room and kicks everyone out.

### Get live updates
`GET /rooms/`
SSE endpoint that streams room state updates. Requires OCCUPANT or OWNER.

Example SSE data:
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

The SSE sends full room state every time instead of diffs. Simpler to implement and handles reconnections better.

## File endpoints

### Upload single file
`POST /files`
Requires: OWNER role
Send as multipart form data with `file` key.

### Upload multiple files
`POST /files/batch`
Requires: OWNER role
Multiple `file` entries in form data.

Returns:
```json
{
  "successfulUploads": [],
  "failedUploads": []
}
```

### Download file
`GET /files/{id}`
Requires: OCCUPANT or OWNER

Returns a download URL that the frontend redirects to:
```json
{
  "downloadUrl": "https://..."
}
```

### Delete files
`DELETE /files`
Requires: OWNER role
Send array of file IDs in request body.

## Metrics
`GET /metrics`
No auth required. Just returns basic usage stats:

```json
{
  "totalRoomsCreated": 3,
  "totalFilesUploaded": 1, 
  "totalFilesDownloaded": 2
}
```

## More notes

- Room codes are 8 characters
- TTL is in minutes
- Everything expires automatically
- Files get signed URLs from Azure for downloads
- SSE connection stays open until room expires or client disconnects
- If you're not the room owner, you can't upload or delete files
- Multiple cleanup layers to make sure nothing gets orphaned/prevent data leaks

## Error handling

The backend returns proper HTTP status codes. Common ones:
- 403: Not room owner
- 401: Invalid session
- 404: Room/file not found
- 500: Something broke server-side

Frontend should handle these and show appropriate messages.

## Future Optimizations(UX Based)
- PWA features (coming soon)
- Backdoor keys for owners to reclaim rooms and password protected rooms(will do this who knows when lol)


## Deployment
This was deployed on azure
https://eventdrop1-bxgbf8btf6aqd3ha.francecentral-01.azurewebsites.net/