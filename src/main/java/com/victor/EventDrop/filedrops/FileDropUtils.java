package com.victor.EventDrop.filedrops;

import com.victor.EventDrop.exceptions.FileDropThresholdExceededException;
import com.victor.EventDrop.exceptions.FileDropUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileDropUtils {

    private final AsyncTaskExecutor asyncTaskExecutor;
    private final FileDropRepository fileDropRepository;

    @Value("${room.file-threshold}")
    private long threshold;

    /**
     * Calculates the total size of all files in a specific room.
     *
     * @param roomCode the room's unique code.
     * @return the total file size in bytes.
     */
    private CompletableFuture<Long> calculateRoomFileSize(String roomCode){
        return CompletableFuture.supplyAsync(() -> {
            return fileDropRepository
                    .findByRoomCode(roomCode)
                    .stream()
                    .filter(fileDrop -> !fileDrop.isDeleted())
                    .map(FileDrop::getFileSize)
                    .reduce(0L, Long::sum
                    );
        }, asyncTaskExecutor);
    }

    /**
     * Validates file upload requirements including size threshold.
     *
     * @param file the file to validate
     * @param roomCode the room code
     * @throws IOException if file cannot be read
     */
    CompletableFuture<Void> validateFileUpload(MultipartFile file, String roomCode, long fileSize) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new FileDropUploadException("File cannot be null or empty");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            throw new FileDropUploadException("File must have a valid filename");
        }

        return validateRoomFileThreshold(roomCode, fileSize);
    }

    /**
     * Checks if a new file will exceed the room's size threshold before uploading.
     *
     * @param roomCode     the room's unique code.
     * @param fileSize the size of the new file.
     */
    private CompletableFuture<Void> validateRoomFileThreshold(String roomCode, long fileSize){
        return calculateRoomFileSize(roomCode).thenAcceptAsync(roomSize -> {
            long expectedRoomSize = roomSize + fileSize;
            if (expectedRoomSize > threshold){
                log.info("Cannot upload this file because it will exceed this room file size threshold");
                throw new FileDropThresholdExceededException("Cannot upload this file because it will exceed this room file size threshold");
            }

        }, asyncTaskExecutor);
    }
}
