package com.victor.EventDrop.rooms.events;

import java.util.UUID;

public record RoomLeaveEvent(
        String roomCode,
        String occupantName,
        UUID sessionId
) {
}
