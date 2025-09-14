package com.victor.EventDrop.rooms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "room")
@Builder
public class Room {
    @Id
    @Indexed
    private String roomCode;
    private String roomName;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    //In Seconds
    @TimeToLive
    private long ttl;
}
