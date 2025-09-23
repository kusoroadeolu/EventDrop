package com.victor.EventDrop.filedrops;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash( value = "fileDrop", timeToLive = 86400)
public class FileDrop {
    @Id
    @Indexed
    private UUID fileId;
    private String originalFileName;
    @Indexed
    private String fileName;

    @Indexed
    private String roomCode;
    private long fileSize;
    private String blobUrl;
    private LocalDateTime uploadedAt;
    private boolean isDeleted;
}
