package com.victor.EventDrop.rooms;

import com.victor.EventDrop.occupants.Occupant;
import com.victor.EventDrop.rooms.dtos.RoomCreateRequestDto;
import com.victor.EventDrop.rooms.dtos.RoomJoinRequestDto;
import com.victor.EventDrop.rooms.dtos.RoomJoinResponseDto;
import com.victor.EventDrop.rooms.dtos.RoomResponseDto;

import java.util.List;

public interface RoomService {
    //Orchestrates the creation of a room
    RoomJoinResponseDto createRoom(RoomCreateRequestDto roomCreateRequestDto);

    //Orchestrates room joins
    RoomJoinResponseDto joinRoom(RoomJoinRequestDto roomJoinRequestDto);

    void leaveRoom(Occupant occupant);

    List<RoomResponseDto> findAllActiveRooms();

    void deleteRoom(Occupant occupant);

    void deleteByRoomCode(String roomCode);
}
