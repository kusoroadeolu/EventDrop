package com.victor.EventDrop.rooms.orchestrators;

import com.victor.EventDrop.filedrops.dtos.FileDropResponseDto;

import java.time.LocalDateTime;
import java.util.List;


public record RoomStateDto(
        String roomName,
        String roomCode,
        List<FileDropResponseDto> fileDrops,
        int occupantCount,
        String notification,
        LocalDateTime expiresAt,
        boolean isExpired
) {
}
