package com.victor.EventDrop.filedrops;

import com.victor.EventDrop.filedrops.dtos.BatchDeleteResult;
import com.victor.EventDrop.filedrops.dtos.BatchUploadResult;
import com.victor.EventDrop.filedrops.dtos.FileDownloadResponseDto;
import com.victor.EventDrop.filedrops.dtos.FileDropResponseDto;
import com.victor.EventDrop.occupants.Occupant;
import com.victor.EventDrop.rooms.events.RoomEvent;
import com.victor.EventDrop.rooms.events.RoomEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/files")
public class FileDropController {

    private final FileDropService fileDropService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<FileDropResponseDto> uploadFile(@AuthenticationPrincipal Occupant occupant, @RequestParam("file") MultipartFile file) {
        var fileDropResponseDto = fileDropService.uploadFile(occupant.getRoomCode(), file);
        fileDropService.publishRoomEvent(new RoomEvent(
                occupant.getOccupantName() + " uploaded a file",
                LocalDateTime.now(),
                RoomEventType.ROOM_FILE_UPLOAD,
                occupant.getRoomCode(),
                1
        ));
        return new ResponseEntity<>(fileDropResponseDto, HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BatchUploadResult> uploadFiles(@AuthenticationPrincipal Occupant occupant, @RequestParam("file") List<MultipartFile> files) {
        var batchUploadResult = fileDropService.uploadFiles(occupant.getRoomCode(), files);

        fileDropService.publishRoomEvent(new RoomEvent(
                String.format("%s uploaded %d files", occupant.getOccupantName(), batchUploadResult.successfulUploads().size()),
                LocalDateTime.now(),
                RoomEventType.ROOM_BATCH_FILE_UPLOAD,
                occupant.getRoomCode(),
                batchUploadResult.successfulUploads().size()

        ));
        return new ResponseEntity<>(batchUploadResult, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OCCUPANT', 'OWNER')")
    public ResponseEntity<FileDownloadResponseDto> downloadFile(@AuthenticationPrincipal Occupant occupant, @PathVariable("id") String fileId) {
        FileDownloadResponseDto downloadUrl = fileDropService.downloadFile(UUID.fromString(fileId), occupant.getRoomCode());
        fileDropService.publishRoomEvent(new RoomEvent(
                null,
                LocalDateTime.now(),
                RoomEventType.ROOM_FILE_DOWNLOAD,
                occupant.getRoomCode(),
                1
        ));
        return ResponseEntity.status(302).body(downloadUrl);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BatchDeleteResult> deleteFiles(@AuthenticationPrincipal Occupant occupant, @RequestBody List<String> fileIds) {
        List<UUID> uuids = fileIds.stream().map(String::trim).map(UUID::fromString).toList();
        var batchDto = fileDropService.deleteFiles(occupant.getRoomCode(), uuids);
        String notification = uuids.size() > 1 ?
                occupant.getOccupantName() + " deleted multiple files" : occupant.getOccupantName() + " deleted a file";

        fileDropService.publishRoomEvent(new RoomEvent(
                notification,
                LocalDateTime.now(),
                RoomEventType.ROOM_BATCH_FILE_DELETE,
                occupant.getRoomCode(),
                null
        ));
        return new ResponseEntity<>(batchDto, HttpStatus.NO_CONTENT);
    }
}