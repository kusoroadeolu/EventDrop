package com.victor.EventDrop.rooms;

import com.victor.EventDrop.occupants.Occupant;
import com.victor.EventDrop.rooms.dtos.RoomCreateRequestDto;
import com.victor.EventDrop.rooms.dtos.RoomJoinRequestDto;
import com.victor.EventDrop.rooms.dtos.RoomJoinResponseDto;
import com.victor.EventDrop.rooms.dtos.RoomResponseDto;

import java.util.List;
import java.util.Optional;

public interface RoomService {
    //Orchestrates the creation of a room
    RoomJoinResponseDto createRoom(RoomCreateRequestDto roomCreateRequestDto);

    //Orchestrates room joins
    RoomJoinResponseDto joinRoom(RoomJoinRequestDto roomJoinRequestDto);

    void leaveRoom(Occupant occupant);


    void deleteRoom(Occupant occupant);

    Optional<Room> findOptionalRoomByRoomCode(String roomCode);

    void deleteByRoomCode(String roomCode);

    Room findByRoomCode(String roomCode);
}
