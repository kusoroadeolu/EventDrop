package com.victor.EventDrop.filedrops;

import com.victor.EventDrop.exceptions.FileDropDownloadException;
import com.victor.EventDrop.exceptions.FileDropThresholdExceededException;
import com.victor.EventDrop.exceptions.FileDropUploadException;
import com.victor.EventDrop.exceptions.NoSuchFileDropException;
import com.victor.EventDrop.filedrops.client.FileDropStorageClient;
import com.victor.EventDrop.filedrops.dtos.BatchDeleteResult;
import com.victor.EventDrop.filedrops.dtos.BatchUploadResult;
import com.victor.EventDrop.filedrops.dtos.FileDownloadResponseDto;
import com.victor.EventDrop.filedrops.dtos.FileDropResponseDto;
import com.victor.EventDrop.rooms.events.RoomEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.StreamSupport;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileDropServiceImpl implements FileDropService {
    private final FileDropRepository fileDropRepository;
    private final FileDropMapper fileDropMapper;
    private final FileDropStorageClient fileDropStorageClient;
    private final FileDropUtils fileDropUtils;
    private final AsyncTaskExecutor asyncTaskExecutor;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Uploads a file and saves its metadata to the database.
     * Uses async operations internally but blocks until completion to maintain security context.
     *
     * @param roomCode the room's unique code.
     * @param file the file to upload.
     * @return a {@link FileDropResponseDto} for the completed upload operation.
     */
    @Override
    public FileDropResponseDto uploadFile(String roomCode, MultipartFile file) {
        try{
            String fileDropName = roomCode + "/" + file.getOriginalFilename();
            InputStream stream = file.getInputStream();
            long fileSize = file.getSize();
            List<FileDrop> fileDrops = fileDropRepository.findByRoomCode(roomCode);

            return fileDropUtils.validateFileUpload(file, fileDrops, fileSize)
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
                                    .fileSize(fileSize)
                                    .roomCode(roomCode)
                                    .blobUrl(blobUrl)
                                    .isDeleted(false)
                                    .uploadedAt(LocalDateTime.now())
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
    @Override
    public BatchUploadResult uploadFiles(String roomCode, List<MultipartFile> files){

        log.info("Starting batch file upload. Upload count: {}", files.size());

        List<FileDrop> fileDrops = fileDropRepository.findByRoomCode(roomCode);

        fileDropUtils.validateBatchUpload(fileDrops, files);

        List<FileDropResponseDto> successfulUploads = new CopyOnWriteArrayList<>();
        List<FileDropResponseDto> failedUploads = new CopyOnWriteArrayList<>();



        var uploadFutures = files.stream()
                .map(file -> CompletableFuture.supplyAsync(() ->
                        uploadFile(
                                roomCode, file
                        ), asyncTaskExecutor)
                        .thenAccept(successfulUploads::add)
                        .exceptionally(throwable -> {
                            log.error("Failed to upload file: {}", file.getOriginalFilename(), throwable);
                            failedUploads.add(new FileDropResponseDto(null, file.getOriginalFilename(), file.getSize() ,LocalDateTime.now()));
                            return null;
                        }
                )).toList();



        CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).join();
        log.info("Successfully batch uploaded all files. Success: {}. Failed: {}", successfulUploads.size(), failedUploads.size());

        return new BatchUploadResult(successfulUploads, failedUploads);

    }


    /**
     * Asynchronously handles a file download request.
     * Finds the file's URL and sends a download event to a room-specific RabbitMQ queue.
     *
     * @param fileDropId the unique ID of the file drop.
     * @param roomCode   the room's unique code.
     * @return a {@link String} for the download event's result.
     */
    @Override
    public FileDownloadResponseDto downloadFile(UUID fileDropId, String roomCode){
        if(roomCode == null || roomCode.isEmpty()){
            log.info("Failed to download file because the room code is empty or null");
            throw new FileDropDownloadException("Failed to download file because the room code is empty or null");
        }

        String downloadUrl = CompletableFuture.supplyAsync(() -> {
                    FileDrop fileDrop = fileDropRepository
                            .findById(fileDropId)
                            .orElseThrow(() -> new NoSuchFileDropException(String.format("Could not find file drop with ID: %s", fileDropId)));
                    log.info("Found file drop with ID: {}. Name: {}", fileDrop, fileDrop.getFileName());
                    if(!fileDrop.getRoomCode().equals(roomCode)){
                        log.info("Cannot download this file because you are not in the same room.");
                        throw new FileDropDownloadException("Cannot download this file because you are not in the same room.");
                    }

                    String fileDropUrl = fileDrop.getBlobUrl();
                    String blobName = fileDrop.getFileName();

                    return fileDropStorageClient.downloadFile(blobName, fileDropUrl);

                }, asyncTaskExecutor)
                .exceptionally(throwable -> {
                    log.info("An unexpected error occurred while trying to download file drop with ID: {}. Error message: {}", fileDropId, throwable.getMessage());
                    throw new FileDropDownloadException(String.format("An unexpected error occurred while trying to download file drop with ID: %s", fileDropId), throwable);
                }).join();

        return new FileDownloadResponseDto(downloadUrl);
    }

    @Override
    public void deleteByRoomCode(String roomCode){
        try{
            fileDropRepository.deleteByRoomCode(roomCode);
            log.info("Successfully deleted all file drops in room with room code: {}", roomCode);
        }catch (Exception e){
            log.error("Failed to delete some file drops in room with room code: {}", roomCode, e);
        }
    }

    /**
     * Asynchronously deletes a batch of files
     *
     * @param roomCode    the room id the files belong to
     * @param fileIds list of file ids to delete
     * @return a CompletableFuture containing a batch result dto of deleted files
     */
    @Override
    public BatchDeleteResult deleteFiles(String roomCode, List<UUID> fileIds) {

        if(fileIds == null){
            log.info("File IDs cannot be null");
            throw new IllegalArgumentException("File IDs cannot be null");
        }

        if(fileIds.isEmpty()){
            log.info("File IDs cannot be empty");
            return new BatchDeleteResult(new ArrayList<>(), new ArrayList<>());
        }

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
                        log.info("Attempting batch delete of {} files for room {}", blobNames.size(), roomCode);
                        fileDropStorageClient.deleteFiles(blobNames);

                        // Remove metadata from DB
                        fileDropRepository.deleteAll(fileDrops);
                        deletedNow.addAll(blobNames);

                    } catch (Exception e) {
                        log.error("Batch delete failed for room {}: {}", roomCode, e.getMessage(), e);

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
                }, asyncTaskExecutor)
                .join();
    }

    @Override
    public List<FileDropResponseDto> getFileDrops(String roomCode){
        return fileDropRepository
                .findByRoomCode(roomCode)
                .stream()
                .filter(fileDrop -> !fileDrop.isDeleted())
                .map(fileDropMapper::toResponseDto)
                .toList();
    }

    @Override
    public void publishRoomEvent(RoomEvent roomEvent){
        applicationEventPublisher.publishEvent(roomEvent);
    }

}