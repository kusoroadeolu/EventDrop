package com.victor.EventDrop.rooms.listeners;

import com.victor.EventDrop.rooms.*;
import com.victor.EventDrop.rooms.configproperties.RoomExpiryConfigProperties;
import com.victor.EventDrop.rooms.events.RoomEvent;
import com.victor.EventDrop.rooms.events.RoomEventType;
import com.victor.EventDrop.rooms.events.RoomExpiryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Listens for Redis key expiration events to handle room cleanup.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoomExpiryListener {

    private final RoomService roomService;
    private final RoomQueueListenerService roomQueueListenerService;
    private final RabbitTemplate rabbitTemplate;
    private final RoomQueueDeclarationService roomQueueDeclarationService;
    private final RoomExpiryConfigProperties roomExpiryConfigProperties;
    private final RoomEmitterHandler roomEmitterHandler;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Handles the expiration of a room key in Redis.
     *
     * @param expiredEvent The Redis key expired event containing the expired Room object.
     */
    @EventListener
    public void handleRoomExpiry(RedisKeyExpiredEvent<Room> expiredEvent){
        byte[] expiredEventId = expiredEvent.getId();
        String roomCode = new String(expiredEventId, StandardCharsets.UTF_8);

        //Since room codes are strings and not UUIDs, return
        if (roomCode.length() > 8){
            return;
        }

        //Send the room event to immediately disconnect users
        applicationEventPublisher.publishEvent(new RoomEvent(
                "Room " + roomCode + " has expired",
                LocalDateTime.now(),
                RoomEventType.ROOM_EXPIRY,
                roomCode,
                null
        ));

        log.info("Handling expired room: {}", roomCode);

        try{
            roomService.deleteByRoomCode(roomCode);
            roomQueueListenerService.stopAllListeners(roomCode);
            roomQueueDeclarationService.deleteAllQueues(roomCode);
            roomEmitterHandler.removeRoomEmitters(roomCode);

            // Publishes a message to RabbitMQ to notify other services of the room's expiration.
            rabbitTemplate.convertAndSend(
                    roomExpiryConfigProperties.getExchangeName(),
                    roomExpiryConfigProperties.getRoutingKey(),
                    new RoomExpiryEvent(roomCode)
            );


        } catch (Exception e){
            log.error("Failed to handle room expiry for room with code: {}. Cause: {}", roomCode, e.getMessage(), e);
        }

    }
}