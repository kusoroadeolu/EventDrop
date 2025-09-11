package com.victor.EventDrop.occupants;

import com.victor.EventDrop.rooms.dtos.RoomJoinEvent;

public interface OccupantService {
    void createOccupant(RoomJoinEvent roomJoinEvent);
}
