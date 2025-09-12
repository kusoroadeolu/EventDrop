package com.victor.EventDrop.rooms.listeners;

import com.victor.EventDrop.rabbitmq.RoomJoinListenerService;
import com.victor.EventDrop.rooms.Room;
import com.victor.EventDrop.rooms.RoomRepository;
import com.victor.EventDrop.rooms.RoomService;
import com.victor.EventDrop.rooms.config.RoomExpiryConfigProperties;
import com.victor.EventDrop.rooms.config.RoomJoinConfigProperties;
import com.victor.EventDrop.rooms.events.RoomExpiryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomExpiryListener {

    private final RoomRepository roomRepository;
    private final RoomService roomService;
    private final RabbitTemplate rabbitTemplate;
    private final RoomExpiryConfigProperties roomExpiryConfigProperties;

    @EventListener
    public void handleRoomExpiry(RedisKeyExpiredEvent<Room> expiredEvent){
        var expiredValue = expiredEvent.getValue();

        if(expiredValue instanceof Room expiredRoom){
            log.info("Handling expired room: {}", expiredRoom.getRoomCode());
            roomRepository.delete(expiredRoom);
            roomService.stopAllListeners(expiredRoom.getRoomCode());
            rabbitTemplate.convertAndSend(
                    roomExpiryConfigProperties.getExchangeName(),
                    roomExpiryConfigProperties.getRoutingKey(),
                    new RoomExpiryEvent(expiredRoom.getRoomCode())
            );
            log.info("Successfully deleted expired room with room code: {}", expiredRoom.getRoomCode());
        }
    }


}
