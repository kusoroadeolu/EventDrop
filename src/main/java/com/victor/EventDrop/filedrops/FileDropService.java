package com.victor.EventDrop.filedrops;

import com.victor.EventDrop.filedrops.dtos.BatchDeleteResult;
import com.victor.EventDrop.filedrops.dtos.BatchUploadResult;
import com.victor.EventDrop.filedrops.dtos.FileDownloadResponseDto;
import com.victor.EventDrop.filedrops.dtos.FileDropResponseDto;
import com.victor.EventDrop.rooms.events.RoomEvent;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FileDropService {
    FileDropResponseDto uploadFile(String roomCode , MultipartFile file);

    BatchUploadResult uploadFiles(String roomCode, List<MultipartFile> files);

    FileDownloadResponseDto downloadFile(UUID fileDropId, String roomCode);

    void deleteByRoomCode(String roomCode);

    BatchDeleteResult deleteFiles(String roomCode, List<UUID> fileIds);

    List<FileDropResponseDto> getFileDrops(String roomCode);

    void publishRoomEvent(RoomEvent roomEvent);
}
