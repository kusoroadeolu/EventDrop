package com.victor.EventDrop.occupants;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "occupant", timeToLive = 300L)
@Builder
public class Occupant {

    @Indexed
    private String roomCode;
    @Id
    private UUID sessionId;
    private String occupantName;
    @Indexed
    private OccupantRole occupantRole;
    private LocalDateTime joinedAt;
    private LocalDateTime roomExpiry;
}
