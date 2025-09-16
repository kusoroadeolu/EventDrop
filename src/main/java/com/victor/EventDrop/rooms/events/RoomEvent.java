package com.victor.EventDrop.rooms.events;


import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.Optional;

public record RoomEvent(
        //This should be displayed on a toast
        String notification,
        LocalDateTime occurredAt,
        RoomEventType roomEventType,
        String roomCode
) {
}