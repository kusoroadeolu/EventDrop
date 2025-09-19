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
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileDropUtils {

    private final AsyncTaskExecutor asyncTaskExecutor;

    @Value("${room.file-size-threshold}")
    private long roomFileSizeThreshold;

    @Value("${room.file-count-threshold}")
    private int roomFileCountThreshold;




    /**
     * Calculates the total size of all files in a specific room.
     *
     * @param fileDrops the file drops in the room
     * @return the total file size in bytes.
     */
    private CompletableFuture<Long> calculateRoomFileSize(List<FileDrop> fileDrops){
        return CompletableFuture.supplyAsync(() -> {
            return fileDrops
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
     * @param fileDrops the file drops in the room
     * @throws IOException if file cannot be read
     */
    CompletableFuture<Void> validateFileUpload(MultipartFile file, List<FileDrop> fileDrops, long fileSize) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new FileDropUploadException("File cannot be null or empty");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            throw new FileDropUploadException("File must have a valid filename");
        }

        return validateRoomFileThreshold(fileDrops, fileSize);
    }

    /**
     * Checks if a new file will exceed the room's size threshold before uploading.
     *
     * @param fileDrops the file drops in the room
     * @param fileSize the size of the new file.
     */
    private CompletableFuture<Void> validateRoomFileThreshold(List<FileDrop> fileDrops, long fileSize){
        return calculateRoomFileSize(fileDrops).thenAcceptAsync(roomSize -> {
            long expectedRoomSize = roomSize + fileSize;

            if (expectedRoomSize > roomFileSizeThreshold){
                log.info("Cannot upload this file because it will exceed this room file size threshold");
                throw new FileDropThresholdExceededException("Cannot upload this file because it will exceed this room file size threshold");
            }

        }, asyncTaskExecutor);
    }

    public void validateBatchUpload(List<FileDrop> fileDrops, List<MultipartFile> files){
        long currentSize = calculateRoomFileSize(fileDrops).join();
        long currentCount = fileDrops.size();

        long batchSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        int batchCount = files.size();

        if((currentSize + batchSize) > roomFileSizeThreshold){
            log.info("Cannot upload these files because they will exceed this room's file size threshold of 2GB");
            throw new FileDropThresholdExceededException("Cannot upload these files because they will exceed this room's file size threshold of 2GB");
        }

        if((currentCount + batchCount) > roomFileCountThreshold){
            log.info("Cannot upload these files because they will exceed this room's file count threshold of {} files", roomFileCountThreshold);
            throw new FileDropThresholdExceededException(String.format("Cannot upload these files because they will exceed this room's file count threshold of %s files", roomFileCountThreshold));
        }
    }
}
