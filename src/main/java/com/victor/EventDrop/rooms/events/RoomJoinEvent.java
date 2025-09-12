package com.victor.EventDrop.rooms.events;

import com.victor.EventDrop.occupants.OccupantRole;

import java.util.UUID;

public record RoomJoinEvent(
        String username,
        UUID sessionId,
        OccupantRole role,
        String roomCode
) {
}
