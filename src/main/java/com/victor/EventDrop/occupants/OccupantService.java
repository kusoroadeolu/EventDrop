package com.victor.EventDrop.occupants;

import com.victor.EventDrop.rooms.events.RoomJoinEvent;
import com.victor.EventDrop.rooms.events.RoomLeaveEvent;

public interface OccupantService {
    void createOccupant(RoomJoinEvent roomJoinEvent);

    //Listens for room join events to create occupants
    void deleteOccupant(RoomLeaveEvent roomLeaveEvent);
}
