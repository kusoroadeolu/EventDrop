package com.victor.EventDrop.filedrops;

import com.victor.EventDrop.rooms.events.RoomEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FileDropService {
    FileDropResponseDto uploadFile(String roomCode, LocalDateTime expiresAt, MultipartFile file);

    BatchUploadResult uploadFiles(String roomCode, LocalDateTime expiresAt, List<MultipartFile> files);

    FileDownloadResponseDto downloadFile(UUID fileDropId, String roomCode);

    void deleteByRoomCode(String roomCode);

    BatchDeleteResult deleteFiles(String roomId, List<UUID> fileIds);

    @Async
    void publishRoomEvent(RoomEvent roomEvent);
}
