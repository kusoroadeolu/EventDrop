package com.victor.EventDrop.rooms.dtos;

import net.minidev.json.annotate.JsonIgnore;

import java.time.LocalDateTime;

public record RoomJoinResponseDto(
        String roomName,
        String username,
        @JsonIgnore
        String sessionId,
        LocalDateTime expiresAt
) {
}
