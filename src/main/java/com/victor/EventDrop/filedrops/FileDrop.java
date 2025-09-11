package com.victor.EventDrop.filedrops;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("fileId")
public class FileDrop {
    @Id
    @Indexed
    private UUID fileId;
    private String fileName;
    private String roomCode;
    private int fileSizeInMB;
}
