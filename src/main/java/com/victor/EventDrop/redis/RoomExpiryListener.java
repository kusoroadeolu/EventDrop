package com.victor.EventDrop.redis;

import com.victor.EventDrop.rabbitmq.RoomJoinListenerService;
import com.victor.EventDrop.rooms.Room;
import com.victor.EventDrop.rooms.RoomRepository;
import com.victor.EventDrop.rooms.config.RoomJoinConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomExpiryListener {

    private final RoomRepository roomRepository;
    private final RoomJoinListenerService roomJoinListenerService;
    private final RoomJoinConfigProperties roomJoinConfigProperties;

    @EventListener
    public void handleExpiredRooms(RedisKeyExpiredEvent<Room> expiredEvent){
        log.info("Handling expired room event...");
            Room expiredRoom = (Room) expiredEvent.getValue();
            if(expiredRoom != null){
                roomRepository.delete(expiredRoom);
                String queueName = roomJoinConfigProperties.getQueuePrefix() + expiredRoom.getRoomCode();
                roomJoinListenerService.stopListener(queueName);
                log.info("Successfully handled expired room: {}", expiredRoom.getRoomCode());
            }
    }

}
