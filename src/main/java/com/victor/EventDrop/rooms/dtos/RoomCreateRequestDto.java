package com.victor.EventDrop.rooms.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RoomCreateRequestDto(
        @NotEmpty(message = "Room name cannot be empty")
        @Size(message = "Room name cannot have less than 3 characters and more than 20 characters", min = 3, max = 20)
        String roomName,
        @Positive(message = "Time to live cannot be negative or zero")
        long ttl,
        @NotEmpty(message = "The room owner's name cannot be empty")
        @Size(message = "The room owner's name cannot have less than 3 characters and more than 20 characters", min = 3, max = 20)
        String username

) {
}
