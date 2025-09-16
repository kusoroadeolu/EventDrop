package com.victor.EventDrop.occupants;

import com.victor.EventDrop.rooms.events.RoomJoinEvent;
import com.victor.EventDrop.rooms.events.RoomLeaveEvent;

public interface OccupantService {
    String createOccupant(RoomJoinEvent roomJoinEvent);

    //Listens for room join events to create occupants
    Boolean deleteOccupant(RoomLeaveEvent roomLeaveEvent);

    int getOccupantCount(String roomCode);
}
