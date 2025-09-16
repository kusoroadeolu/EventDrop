package com.victor.EventDrop.filedrops;

import com.victor.EventDrop.filedrops.client.FileDropStorageClient;
import com.victor.EventDrop.rooms.RoomServiceImpl;
import com.victor.EventDrop.rooms.events.RoomExpiryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Event listener for handling file drop-related cleanup operations.
 * This class orchestrates the deletion of files and their metadata in response to various events.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class FileDropEventListener {

    private final FileDropRepository fileDropRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FileDropStorageClient fileDropStorageClient;

    /**
     * Listens for room expiry events to trigger a cascading cleanup of all associated file drops.
     * It sets a short TTL on file drop keys in Redis and initiates the deletion of the files
     * from Azure Blob Storage.
     *
     * @param roomExpiryEvent The event containing the room code of the expired room.
     */
    @RabbitListener(queues = "${room.expiry.queue-name}")
    public void handleRoomExpiry(RoomExpiryEvent roomExpiryEvent){
        String roomCode = roomExpiryEvent.roomCode();
        List<FileDrop> fileDrops = fileDropRepository.findByRoomCode(roomCode);

        if(fileDrops.isEmpty())return;


        log.info("Handling room expiry for file drops for room with room code: {}", roomCode);
        fileDrops
                .parallelStream()
                .forEach(fileDrop -> {
                    redisTemplate.expire(fileDrop.getFileId().toString(), Duration.ofSeconds(2));
                });

        List<String> fileNames = fileDrops.stream()
                .map(FileDrop::getFileName).toList();
        fileDropStorageClient.deleteFiles(fileNames);
    }


    /**
     * Listens for Redis key expiration events for 'FileDrop' keys.
     * When a key expires, this method deletes the corresponding file drop
     * from the database using the file ID extracted from the expired key.
     *
     * @param expiredEvent The event containing the expired Redis key.
     */
    @EventListener
    public void handleExpiredKeys(RedisKeyExpiredEvent<FileDrop> expiredEvent){
        byte[] keyBytes = expiredEvent.getId();
        if (keyBytes.length == 0) {
            log.warn("Received Redis expiry event with null or empty key");
            return;
        }

        String fileId = new String(keyBytes, StandardCharsets.UTF_8).trim();

        if (fileId.length() <= 8) {
            log.warn("Received Redis expiry event with invalid file ID");
            return;
        }

        UUID uuid;

        try{
            uuid = UUID.fromString(fileId);
        }catch (IllegalArgumentException e){
            log.info("Invalid UUID: {}", fileId);
            return;
        }

        try {

            fileDropRepository.deleteById(uuid);
            log.info("Successfully deleted expired file drop with ID: {}", fileId);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for expired file drop ID: {}", fileId, e);
        } catch (Exception e) {
            log.error("Failed to delete expired file drop with ID: {}", fileId, e);
        }
    }

}