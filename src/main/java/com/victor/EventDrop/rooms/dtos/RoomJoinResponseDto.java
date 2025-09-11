package com.victor.EventDrop.rooms.dtos;

import java.time.LocalDateTime;

public record RoomJoinResponseDto(
        String roomCode,
        String roomName,
        String username,
        String sessionId,
        LocalDateTime expiresAt
) {
}
