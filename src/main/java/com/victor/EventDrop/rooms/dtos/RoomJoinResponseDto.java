package com.victor.EventDrop.rooms.dtos;

import java.time.LocalDateTime;

public record RoomJoinResponseDto(
        String roomName,
        String username,
        String sessionId,
        LocalDateTime expiresAt
) {
}
