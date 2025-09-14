package com.victor.EventDrop.filedrops;

import com.victor.EventDrop.exceptions.FileDropDownloadException;
import com.victor.EventDrop.exceptions.FileDropThresholdExceededException;
import com.victor.EventDrop.exceptions.FileDropUploadException;
import com.victor.EventDrop.exceptions.NoSuchFileDropException;
import com.victor.EventDrop.filedrops.client.FileDropStorageClient;
import com.victor.EventDrop.rooms.configproperties.RoomFileUploadConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.StreamSupport;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileDropServiceImpl {
        private final FileDropRepository fileDropRepository;
        private final FileDropMapper fileDropMapper;
        private final FileDropStorageClient fileDropStorageClient;
        private final RabbitTemplate rabbitTemplate;
        private final RoomFileUploadConfigProperties roomFileUploadConfigProperties;
        private final AsyncTaskExecutor asyncTaskExecutor;


        @Value("${room.file-threshold}")
        private int thresholdInMb;

    /**
     * Uploads a file and saves its metadata to the database.
     * Uses async operations internally but blocks until completion to maintain security context.
     *
     * @param roomCode the room's unique code.
     * @param expiresAt when the file should expire
     * @param file the file to upload.
     * @return a {@link FileDropResponseDto} for the completed upload operation.
     */
    public FileDropResponseDto uploadFile(String roomCode, LocalDateTime expiresAt , MultipartFile file) {
        try{
            String fileDropName = roomCode + "/" + file.getOriginalFilename();
            BigDecimal fileSizeInMb = BigDecimal
                           .valueOf(file.getSize())
                           .divide(new BigDecimal(1024 * 1024), 2, RoundingMode.HALF_UP);
            InputStream stream = file.getInputStream();

            return validateFileUpload(file, roomCode, fileSizeInMb)
                    .thenComposeAsync(i -> {
                        try {
                            return fileDropStorageClient.uploadFile(fileDropName, file.getSize() , stream);
                        } catch (IOException e) {
                            log.info("Failed to upload file due to an IO Exception", e);
                            throw new FileDropUploadException("Failed to upload file due to an IO Exception", e);
                        }
                    })
                    .thenApplyAsync(blobUrl -> {
                       try{
                           UUID fileDropId = UUID.randomUUID();
                           FileDrop fileDrop = FileDrop
                                   .builder()
                                   .fileId(fileDropId)
                                   .originalFileName(file.getOriginalFilename())
                                   .fileName(fileDropName)
                                   .fileSizeInMB(fileSizeInMb)
                                   .roomCode(roomCode)
                                   .blobUrl(blobUrl)
                                   .uploadedAt(LocalDateTime.now())
                                   .ttlInSeconds(Duration.between(LocalDateTime.now(), expiresAt).getSeconds())
                                   .build();

                           FileDrop saved = fileDropRepository.save(fileDrop);
                           return fileDropMapper.toResponseDto(saved);
                       }catch (Exception e){
                           log.error("An unexpected error occurred while trying to upload file drop: {}", fileDropName, e);
                           throw new FileDropUploadException(String.format("An unexpected error occurred while trying to upload file drop: %s", fileDropName), e);
                       }
                       }, asyncTaskExecutor).join();
        }catch (IOException e){
            throw new FileDropUploadException("Failed to upload file due to an IO Exception", e);
        }
    }

    /**
     * Asynchronously uploads a list of files.
     * @param roomCode the room's unique code.
     * @param files the list of files to upload.
     * @return a CompletableFuture with a list of successfully uploaded file DTOs.
     */
    public CompletableFuture<List<FileDropResponseDto>> uploadFilesAsync(String roomCode, LocalDateTime expiresAt ,List<MultipartFile> files){
        List<FileDropResponseDto> fileDrops = files
                .stream()
                .map(file -> uploadFile(roomCode, expiresAt ,file))
                .toList();

        return CompletableFuture.allOf(fileDrops.toArray(new CompletableFuture[0]))
                .thenApply(ignored ->
                        fileDrops
                                .stream()
                                .map(future -> {
                                    try{
                                        return future;
                                    }catch (Exception e){
                                        log.error("An unexpected error occurred while trying to upload file drop");
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .toList()
                );
    }

    /**
     * Asynchronously handles a file download request.
     * Finds the file's URL and sends a download event to a room-specific RabbitMQ queue.
     *
     * @param fileDropId the unique ID of the file drop.
     * @param roomCode   the room's unique code.
     * @return a {@link CompletableFuture} for the download event's result.
     */
    public CompletableFuture<String> downloadFileAsync(UUID fileDropId, String roomCode){
            if(roomCode == null || roomCode.isEmpty()){
                log.info("Failed to download file because the room code is empty or null");
                throw new FileDropDownloadException("Failed to download file because the room code is empty or null");
            }

            return CompletableFuture.supplyAsync(() -> {
                    FileDrop fileDrop = fileDropRepository
                            .findById(fileDropId)
                            .orElseThrow(() -> new NoSuchFileDropException(String.format("Could not find file drop with ID: %s", fileDropId)));
                    log.info("Found file drop with ID: {}. Name: {}", fileDrop, fileDrop.getFileName());
                    if(!fileDrop.getRoomCode().equals(roomCode)){
                        log.info("Cannot download this file because you are not in the same room.");
                        throw new FileDropDownloadException("Cannot download this file because you are not in the same room.");
                    }

                    String fileDropUrl = fileDrop.getBlobUrl();

                    return fileDropStorageClient.downloadFile(fileDropUrl);

            }, asyncTaskExecutor)
                    .exceptionally(throwable -> {
                        log.info("An unexpected error occurred while trying to download file drop with ID: {}. Error message: {}", fileDropId, throwable.getMessage());
                        throw new FileDropDownloadException(String.format("An unexpected error occurred while trying to download file drop with ID: %s", fileDropId), throwable);
                    });
    }

    /**
     * Asynchronously deletes a batch of files
     *
     * @param roomId    the room id the files belong to
     * @param fileIds list of file ids to delete
     * @return a CompletableFuture containing a batch result dto of deleted files
     */
    public CompletableFuture<BatchDeleteResult> deleteFilesAsync(String roomId, List<UUID> fileIds) {

        List<FileDrop> fileDrops = StreamSupport.stream(
                        fileDropRepository.findAllById(fileIds).spliterator(), false)
                .toList();

        List<String> deletedNow = new CopyOnWriteArrayList<>();
        List<String> markedDeleted = new CopyOnWriteArrayList<>();

        // Prepare blob names
        List<String> blobNames = fileDrops.stream()
                .map(FileDrop::getFileName)
                .toList();

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Attempting batch delete of {} files for room {}", blobNames.size(), roomId);
                fileDropStorageClient.deleteFiles(blobNames);

                // Remove metadata from DB
                fileDropRepository.deleteAll(fileDrops);
                deletedNow.addAll(blobNames);

            } catch (Exception e) {
                log.error("Batch delete failed for room {}: {}", roomId, e.getMessage(), e);

                fileDrops.forEach(fileDrop -> CompletableFuture.runAsync(() -> {
                    try {
                        fileDrop.setDeleted(true);
                        fileDropRepository.save(fileDrop);
                        markedDeleted.add(fileDrop.getFileName());
                    } catch (Exception inner) {
                        log.error("Failed to mark file {} as deleted", fileDrop.getFileName(), inner);
                    }
                }, asyncTaskExecutor));
            }

            return new BatchDeleteResult(new ArrayList<>(deletedNow), new ArrayList<>(markedDeleted));
        }, asyncTaskExecutor);
    }



    @EventListener(condition = "#expiredEvent.source.toString().contains('FileDrop')")
    public void handleExpiredKeys(RedisKeyExpiredEvent<FileDrop> expiredEvent){
        byte[] keyBytes = expiredEvent.getId();
        String fileId = new String(keyBytes, StandardCharsets.UTF_8);

        try {
            fileDropRepository.deleteById(UUID.fromString(fileId));
            log.info("Successfully deleted expired file drop with ID: {}", fileId);
        } catch (Exception e) {
            log.error("Failed to delete expired file drop with ID: {}", fileId, e);
        }
    }




    /**
     * Calculates the total size of all files in a specific room.
     *
     * @param roomCode the room's unique code.
     * @return the total file size in MB.
     */
    private CompletableFuture<BigDecimal> calculateRoomFileSize(String roomCode){
            return CompletableFuture.supplyAsync(() -> {
                return fileDropRepository
                    .findByRoomCode(roomCode)
                    .stream()
                    .map(FileDrop::getFileSizeInMB)
                    .reduce(BigDecimal.ZERO, BigDecimal::add
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
    private CompletableFuture<Void> validateFileUpload(MultipartFile file, String roomCode, BigDecimal fileSizeInMb) throws IOException {

             if (file == null || file.isEmpty()) {
                 throw new FileDropUploadException("File cannot be null or empty");
             }

             if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
                 throw new FileDropUploadException("File must have a valid filename");
             }

            return validateRoomFileThreshold(roomCode, fileSizeInMb);
    }

    /**
     * Checks if a new file will exceed the room's size threshold before uploading.
     *
     * @param roomCode     the room's unique code.
     * @param fileSizeInMb the size of the new file.
     */
    private CompletableFuture<Void> validateRoomFileThreshold(String roomCode, BigDecimal fileSizeInMb){
        return calculateRoomFileSize(roomCode).thenApplyAsync(roomSize -> {
               BigDecimal expectedRoomSize = roomSize.add(fileSizeInMb);
               BigDecimal maxThreshold = new BigDecimal(thresholdInMb);

               if (expectedRoomSize.compareTo(maxThreshold) > 0){
                   log.info("Cannot upload this file because it will exceed this room file size threshold");
                   throw new FileDropThresholdExceededException("Cannot upload this file because it will exceed this room file size threshold");
               }

            return null;
        }, asyncTaskExecutor);
    }

}
