package com.victor.EventDrop.orchestrators;

import com.victor.EventDrop.filedrops.dtos.FileDropResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public record RoomStateDto(
        String roomName,
        String roomCode,
        List<FileDropResponseDto> fileDrops,
        int occupantCount,
        //I want this to be displayed as a toast
        String notification,
        LocalDateTime expiresAt,
        //When the room state was last updated
        LocalDateTime lastUpdated
) {
}
