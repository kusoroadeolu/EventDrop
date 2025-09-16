package com.victor.EventDrop.rooms.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RoomCreateRequestDto(
        @NotNull(message = "Room name cannot be null")
        @NotEmpty(message = "Room name cannot be empty")
        @Size(message = "Room name cannot have less than 3 characters and more than 20 characters", min = 3, max = 20)
        String roomName,
        //TTL(Time to live) In minutes
        @Positive(message = "Time to live cannot be negative or zero")
        double ttl,
        @NotNull(message = "Your username cannot be null")
        @NotEmpty(message = "Your username cannot be empty")
        @Size(message = "Your username cannot have less than 3 characters and more than 20 characters", min = 3, max = 20)
        String username

) {
}
