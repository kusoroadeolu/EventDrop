package com.victor.EventDrop.occupants;

import com.victor.EventDrop.exceptions.OccupantCreationException;
import com.victor.EventDrop.rooms.events.RoomExpiryEvent;
import com.victor.EventDrop.rooms.events.RoomJoinEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;
import org.springframework.stereotype.Service;

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


    //Listens for room expiry events to delete occupants.
    //This will occur when a room expires but occupant sessions are still active
    @RabbitListener(queues = "${room.expiry.queue-name}")
    public void handleRoomExpiry(RoomExpiryEvent roomExpiryEvent){
        String roomCode = roomExpiryEvent.roomCode();
        log.info("Handling room expiry for occupants for room with room code: {}", roomCode);
        occupantRepository.deleteByRoomCode(roomCode);
        log.info("Successfully deleted all occupants in room with room code: {}", roomCode);
    }

    //Listens for session expiry to delete occupants
    @EventListener
    public void handleSessionExpiry(RedisKeyExpiredEvent<Occupant> expiredEvent){
        Object expiredValue = expiredEvent.getValue();
        if(expiredValue instanceof Occupant expiredOccupant){
            log.info("Handling expired occupant session");
            occupantRepository.delete(expiredOccupant);
            log.info("Successfully handled expired occupant session");
        }

    }
}
