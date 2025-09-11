package com.victor.EventDrop.rooms.dtos;

public record RoomResponseDto(
        String roomCode,
        String roomName,
        long ttlInMinutes
) {
}
