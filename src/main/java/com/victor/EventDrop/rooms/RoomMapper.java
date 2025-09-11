package com.victor.EventDrop.rooms;

import com.victor.EventDrop.rooms.dtos.RoomCreateRequestDto;
import com.victor.EventDrop.rooms.dtos.RoomJoinResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Service;

@Mapper(componentModel = "spring")
@Service
public interface RoomMapper {


    @Mapping(source = "sessionId", target = "sessionId")
    @Mapping(source = "username", target = "username")
    RoomJoinResponseDto toRoomJoinResponseDto(Room room, String sessionId, String username);


    Room toRoomEntity(RoomCreateRequestDto roomCreateRequestDto);

}