package com.victor.EventDrop.rooms;

import com.victor.EventDrop.occupants.OccupantRole;
import com.victor.EventDrop.rooms.dtos.RoomCreateRequestDto;
import com.victor.EventDrop.rooms.dtos.RoomJoinRequestDto;
import com.victor.EventDrop.rooms.dtos.RoomJoinResponseDto;
import com.victor.EventDrop.rooms.dtos.RoomResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/create")
    public ResponseEntity<RoomJoinResponseDto> createRoom(@Valid @RequestBody RoomCreateRequestDto requestDto){
        RoomJoinResponseDto responseDto = roomService.createRoom(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @PostMapping("/join")
    public ResponseEntity<RoomJoinResponseDto> joinRoom(@Valid @RequestBody RoomJoinRequestDto roomJoinRequestDto){
        RoomJoinResponseDto responseDto = roomService.joinRoom(roomJoinRequestDto);
        roomJoinRequestDto.setRole(OccupantRole.OWNER);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<RoomResponseDto>> getAllRooms(){
        var rooms = roomService.findAllActiveRooms();
        return new ResponseEntity<>(rooms, HttpStatus.OK);
    }

}
