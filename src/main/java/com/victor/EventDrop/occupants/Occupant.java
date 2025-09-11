package com.victor.EventDrop.occupants;

import com.victor.EventDrop.rooms.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "sessionId", timeToLive = 300L)
@Builder
public class Occupant {
    private String roomCode;

    @Id
    private UUID sessionId;
    private String occupantName;
    private OccupantRole occupantRole;
    private LocalDateTime joinedAt;
}
