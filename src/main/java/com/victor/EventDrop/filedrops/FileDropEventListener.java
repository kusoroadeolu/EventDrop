package com.victor.EventDrop.filedrops;

import com.victor.EventDrop.rooms.RoomServiceImpl;
import com.victor.EventDrop.rooms.events.RoomExpiryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class FileDropEventListener {

    private final FileDropServiceImpl fileDropService;

    //Listens for room expiry events to delete occupants.
    //This will occur when a room expires but occupant sessions are still active
    @RabbitListener(queues = "${room.expiry.queue-name}")
    public void handleRoomExpiry(RoomExpiryEvent roomExpiryEvent){
        String roomCode = roomExpiryEvent.roomCode();
        log.info("Handling room expiry for file drops for room with room code: {}", roomCode);
        fileDropService.deleteByRoomCode(roomCode);
    }


//    @EventListener(condition = "#expiredEvent.source.toString().contains('FileDrop')")
//    public void handleExpiredKeys(RedisKeyExpiredEvent<FileDrop> expiredEvent){
//        byte[] keyBytes = expiredEvent.getId();
//        String fileId = new String(keyBytes, StandardCharsets.UTF_8);
//
//        try {
//            fileDropRepository.deleteById(UUID.fromString(fileId));
//            log.info("Successfully deleted expired file drop with ID: {}", fileId);
//        } catch (Exception e) {
//            log.error("Failed to delete expired file drop with ID: {}", fileId, e);
//        }
//    }

}
