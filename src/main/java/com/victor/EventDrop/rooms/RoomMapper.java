package com.victor.EventDrop.rooms;

import com.victor.EventDrop.rooms.dtos.RoomJoinResponseDto;
import org.springframework.stereotype.Service;

@Service
public class RoomMapper {
    public RoomJoinResponseDto toRoomJoinResponseDto(Room room, String sessionId, String username){
        return new RoomJoinResponseDto(
                room.getRoomCode(),
                room.getRoomName(),
                username,
                sessionId,
                room.getExpiresAt()
        );
    }
}