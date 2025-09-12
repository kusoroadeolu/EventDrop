package com.victor.EventDrop.occupants;

import com.victor.EventDrop.rooms.events.RoomJoinEvent;

public interface OccupantService {
    void createOccupant(RoomJoinEvent roomJoinEvent);
}
