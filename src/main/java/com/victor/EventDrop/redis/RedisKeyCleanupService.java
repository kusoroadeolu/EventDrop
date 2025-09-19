package com.victor.EventDrop.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.KeyScanOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisKeyCleanupService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Async
    @Scheduled(cron = "${spring.redis.other.cleanup-cron}")
    public void cleanupOrphanedData() {
        ScanOptions scanOptions = KeyScanOptions
                .scanOptions(DataType.HASH)
                .count(100)
                .match("*")
                .build();

        log.info("Starting scheduled Redis key cleanup for orphaned data.");
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            int keysProcessed = 0;
            while (cursor.hasNext()) {
                String key = cursor.next();
                if (key != null && !key.startsWith("metrics:")) {
                    long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

                    if (ttl == -1) {
                        log.warn("Found orphaned key with no expiry: {}. Setting a 2-second TTL for cleanup.", key);
                        redisTemplate.expire(key, 2, TimeUnit.SECONDS);
                        keysProcessed++;
                    }

                }
            }

            log.info("Finished Redis key cleanup. Processed {} orphaned keys.", keysProcessed);
        } catch (Exception e) {
            log.error("An error occurred during the Redis key cleanup task.", e);
        }
    }
}