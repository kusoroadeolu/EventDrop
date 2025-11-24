package com.victor.EventDrop.metrics;

import com.victor.EventDrop.rooms.events.RoomEvent;
import com.victor.EventDrop.rooms.events.RoomEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimpleMetricsService implements CommandLineRunner {
    private final RedisTemplate<String, Object> redisTemplate;

    @EventListener
    public void logMetrics(RoomEvent roomEvent){
        RoomEventType eventType = roomEvent.roomEventType();

        switch (eventType){
            case ROOM_CREATE -> {
                redisTemplate.opsForValue().increment("metrics:1:roomsCreated", 1);
            }
            case ROOM_FILE_UPLOAD -> {
                redisTemplate.opsForValue().increment("metrics:1:filesUploaded", 1);
            }
            case ROOM_BATCH_FILE_UPLOAD -> {
                redisTemplate.opsForValue().increment("metrics:1:filesUploaded", roomEvent.count());
            }
            case ROOM_FILE_DOWNLOAD -> {
                redisTemplate.opsForValue().increment("metrics:1:filesDownloaded", 1);
            }
            default -> {
                return;
            }
        }

        log.info("Successfully incremented simple metrics");
    }

    public SimpleMetricsDto metricsDto(){
        Integer roomsCreated = (Integer) redisTemplate.opsForValue().get("metrics:1:roomsCreated");
        Integer filesUploaded = (Integer) redisTemplate.opsForValue().get("metrics:1:filesUploaded");
        Integer filesDownloaded = (Integer) redisTemplate.opsForValue().get("metrics:1:filesDownloaded");

        if(filesDownloaded == null){
            filesDownloaded = 0;
        }

        if(filesUploaded == null){
            filesUploaded = 0;
        }

        if (roomsCreated == null){
            roomsCreated = 0;
        }

        return new SimpleMetricsDto(roomsCreated, filesUploaded, filesDownloaded);
    }


    @Override
    public void run(String... args) throws Exception {
        SimpleMetrics metrics = SimpleMetrics
                .builder()
                .roomsCreated(0)
                .filesUploaded(0)
                .filesDownloaded(0)
                .build();
        redisTemplate.opsForValue().setIfAbsent("metrics:1", metrics);
        log.info("Successfully created metrics entity");
    }
}
