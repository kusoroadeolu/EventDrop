package com.victor.EventDrop.occupants;

import com.victor.EventDrop.exceptions.OccupantCreationException;
import com.victor.EventDrop.exceptions.OccupantDeletionException;
import com.victor.EventDrop.rooms.events.RoomExpiryEvent;
import com.victor.EventDrop.rooms.events.RoomJoinEvent;
import com.victor.EventDrop.rooms.events.RoomLeaveEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class OccupantServiceImpl implements OccupantService {

    private final OccupantRepository occupantRepository;

    //Listens for room join events to create occupants
    @Override
    public void createOccupant(RoomJoinEvent roomJoinEvent){
        log.info("Initiating room occupant creation for room: {}", roomJoinEvent.roomCode());
        Occupant occupant = Occupant
                .builder()
                .occupantName(roomJoinEvent.username())
                .roomCode(roomJoinEvent.roomCode())
                .occupantRole(roomJoinEvent.role())
                .sessionId(roomJoinEvent.sessionId())
                .roomExpiry(roomJoinEvent.roomExpiry())
                .build();

        try {
            log.info("Attempting to save occupant: {}", occupant.getOccupantName());
            occupantRepository.save(occupant);
            log.info("Successfully saved occupant: {}", occupant.getOccupantName());
        }catch (Exception e){
            log.info("An unexpected error occurred while trying to save occupant: {}", occupant.getOccupantName(), e);
            throw new OccupantCreationException(String.format("An unexpected error occurred while trying to save occupant: %s", occupant.getOccupantName()), e);
        }

    }

    //Listens for room join events to create occupants
    @Override
    public void deleteOccupant(RoomLeaveEvent roomLeaveEvent){
        log.info("Initiating room occupant deletion for room: {}. Occupant name: {}", roomLeaveEvent.roomCode(), roomLeaveEvent.occupantName());
        Occupant occupant = occupantRepository.findBySessionId(roomLeaveEvent.sessionId().toString());
        if(occupant != null){
            try {
                log.info("Attempting to deleted occupant: {}", occupant.getOccupantName());
                occupantRepository.deleteByRoomCodeAndSessionId(occupant.getRoomCode(), occupant.getSessionId().toString());
                log.info("Successfully deleted occupant: {}", occupant.getOccupantName());
            } catch (Exception e) {
                log.info("An unexpected error occurred while trying to delete occupant: {}", occupant.getOccupantName(), e);
                throw new OccupantDeletionException(String.format("An unexpected error occurred while trying to delete occupant: %s", occupant.getOccupantName()), e);
            }
        }

    }


    //Listens for room expiry events to delete occupants.
    //This will occur when a room expires but occupant sessions are still active
    @RabbitListener(queues = "${room.expiry.queue-name}")
    public void handleRoomExpiry(RoomExpiryEvent roomExpiryEvent){
        String roomCode = roomExpiryEvent.roomCode();
        log.info("Handling room expiry for occupants for room with room code: {}", roomCode);

        try{
            occupantRepository.deleteByRoomCode(roomExpiryEvent.roomCode());
            log.info("Successfully deleted all occupants in room with room code: {}", roomCode);
        }catch (Exception e){
            log.error("Failed to delete expired occupants in room with room code: {}", roomCode, e);
        }

    }

    //Listens for session expiry to delete occupants
    @EventListener
    public void handleSessionExpiry(RedisKeyExpiredEvent<Occupant> expiredEvent){
        byte[] keyBytes = expiredEvent.getId();
        String sessionId = new String(keyBytes, StandardCharsets.UTF_8);

        if (sessionId.isEmpty()){
            return;
        }

        log.info("Session ID: {}", sessionId);

        try{
            occupantRepository.deleteBySessionId(sessionId);
            log.info("Successfully deleted expired occupant with session ID: {}", sessionId);

        }catch (Exception e){
            log.error("Failed to delete expired occupant with session ID: {}", sessionId, e);
        }


    }
}
