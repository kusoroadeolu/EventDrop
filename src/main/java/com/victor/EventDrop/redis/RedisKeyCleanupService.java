package com.victor.EventDrop.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisKeyCleanupService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedRate = 20000)
    public void cleanupOrphanedData(){
        ScanOptions scanOptions = ScanOptions
                .scanOptions()
                .count(100)
                .match("*")
                .build();

        try(Cursor<String> cursor = redisTemplate.scan(scanOptions)){
            while (cursor.hasNext()){
                String key = cursor.next();
                if(key != null){
                    long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

                    if(ttl <= 0){
                        if(key.startsWith("metrics:")){
                            continue;
                        }
                    log.info("Expiring key: {}", key);



                    //Set the timeout to 2 seconds so the event listener can handle its expiry
                    redisTemplate.expire(key, 2, TimeUnit.SECONDS);
                    }
                }
            }
        }catch (Exception e){
            log.error("An error occurred while trying to clean up a redis key");
        }
    }
}
