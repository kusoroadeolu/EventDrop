package com.victor.EventDrop.occupants;

import com.victor.EventDrop.exceptions.OccupantDeletionException;
import com.victor.EventDrop.rooms.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
@Service
public class OccupantServiceImpl implements OccupantService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OccupantRepository occupantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${room.max-size}")
    private int maxRoomSize;

    /**
     * A listener method which listens for {@link RoomJoinEvent} events to create occupants for the room
     * @param roomJoinEvent The event containing metadata relating to room joins
     * @return A join response indicating if the occupant object was created successfully
     * */
    @Override
    public OccupantRoomJoinResponse createOccupant(RoomJoinEvent roomJoinEvent){
        log.info("Initiating room occupant creation for room: {}", roomJoinEvent.roomCode());

        List<Occupant> occupants = occupantRepository.findByRoomCode(roomJoinEvent.roomCode());
        int size = occupants.size();

        if(size >= maxRoomSize){
            log.info("Join request failed because room is already full");
            return new OccupantRoomJoinResponse(false, 409);
        }

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
            return new OccupantRoomJoinResponse(true, 200);
        }catch (Exception e){
            log.info("An unexpected error occurred while trying to save occupant: {}", occupant.getOccupantName(), e);
            return new OccupantRoomJoinResponse(false, 500);
        }

    }

    /**
     * A listener method which listens for {@link RoomLeaveEvent} events to delete occupants
     * @param roomLeaveEvent The event containing metadata relating to room leave
     * */
    @Override
    @RabbitListener(queues = "${room.leave.queue-name}")
    public void deleteOccupant(RoomLeaveEvent roomLeaveEvent){
        String name = roomLeaveEvent.occupantName(), session = roomLeaveEvent.sessionId().toString(), roomCode = roomLeaveEvent.roomCode();

        log.info("Initiating  occupant deletion for room: {}. Occupant name: {}", roomCode, name);
            try {
                log.info("Attempting to deleted occupant: {}", name);
                occupantRepository.deleteByRoomCodeAndSessionId(roomLeaveEvent.roomCode(), session); //I'm not expiring here for instant updates
                log.info("Successfully deleted occupant: {}", roomLeaveEvent.occupantName());
                this.eventPublisher.publishEvent(
                        new RoomEvent(
                                name + " left the room",
                                LocalDateTime.now(),
                                RoomEventType.ROOM_LEAVE,
                                roomCode,
                                null
                        )
                ); //Publish an event after leave
            } catch (Exception e) {
                log.info("An unexpected error occurred while trying to delete occupant: {}", roomLeaveEvent.occupantName(), e);
                throw new OccupantDeletionException(String.format("An unexpected error occurred while trying to delete occupant: %s", name), e);
            }

    }

    /**
     * Gets the count of all occupants in a room
     * @param roomCode The room code of the room
     * @return The count of all occupants in a room
     * */
    @Override
    public int getOccupantCount(String roomCode){
        return occupantRepository.findByRoomCode(roomCode)
                 .size();
    }

    /**
     * A listener method which listens for room expiry events to expire occupants
     * The listener listens for {@link RoomExpiryEvent}, then finds all the occupants in a room
     * and sets their TTL(Time to live) to 2 seconds to allows redis expiration to handle expired values
     *
     * @param roomExpiryEvent The event containing metadata relating to room expiry
     * */
    @RabbitListener(queues = "${room.expiry.queue-name}")
    public void handleRoomExpiry(RoomExpiryEvent roomExpiryEvent){
        String roomCode = roomExpiryEvent.roomCode();
        log.info("Handling room expiry for occupants for room with room code: {}", roomCode);


        try {
            List<Occupant> occupants = occupantRepository.findByRoomCode(roomExpiryEvent.roomCode());
            log.debug("Found {} occupants for room expiry cleanup", occupants.size());


            if (occupants.isEmpty()) {
                log.info("No occupants to process found. Returning...");
                return;
            }

            occupants.parallelStream()
                    .forEach(
                            occupant -> redisTemplate.expire(occupant.getSessionId().toString(), Duration.ofSeconds(2))
                    );

            log.info("Successfully expired all occupants in room with room code: {}", roomCode);
        }catch (ListenerExecutionFailedException e){
            log.error("Listener execution failed while trying to delete occupants in room with code: {}", roomCode, e);
            throw e;
        }catch (Exception e){
            log.error("Failed to delete expired occupants in room with room code: {}", roomCode, e);
            throw new AmqpRejectAndDontRequeueException(String.format("Failed to delete expired occupants in room with room code: %s", roomCode), e);
        }

    }

    /**
     * A listener method that handles Redis key expiration events for occupants.
     * When an occupant's key expires in Redis, this method is triggered to
     * delete the corresponding occupant record from the repository.
     *
     * @param expiredEvent The event containing metadata about the expired occupant's key.
     */
    @EventListener
    public void handleSessionExpiry(RedisKeyExpiredEvent<Occupant> expiredEvent){
        byte[] keyBytes = expiredEvent.getId();

        if (keyBytes.length == 0) {
            log.warn("Received Redis expiry event with null or empty key");
            return;
        }

        String sessionId = new String(keyBytes, StandardCharsets.UTF_8);

        if (sessionId.length() <= 8){
            log.warn("Received Redis expiry event with invalid session ID");
            return;
        }

        try{
            UUID.fromString(sessionId);
        }catch (IllegalArgumentException e){
            log.info("Invalid UUID: {}", sessionId);
            return;
        }

        try {
            occupantRepository.deleteBySessionId(sessionId);
            log.info("Successfully deleted expired occupant with session ID: {}", sessionId);
        }catch (Exception e){
            log.error("Failed to delete expired occupant with session ID: {}", sessionId, e);
        }

    }
}
