package com.victor.EventDrop.rooms;

import com.victor.EventDrop.occupants.Occupant;
import com.victor.EventDrop.occupants.OccupantRole;
import com.victor.EventDrop.rooms.listeners.RoomEventListener;
import com.victor.EventDrop.rooms.dtos.RoomCreateRequestDto;
import com.victor.EventDrop.rooms.dtos.RoomJoinRequestDto;
import com.victor.EventDrop.rooms.dtos.RoomJoinResponseDto;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;


@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;
    private final RoomEventListener roomEventListener;
    private final RoomEmitterHandler roomEmitterHandler;
    private final CookieUtils cookieUtils;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, SseEmitter>> sseEmitters;


    @PostMapping("/create")
    public ResponseEntity<RoomJoinResponseDto> createRoom(@Valid @RequestBody RoomCreateRequestDto requestDto, HttpServletResponse response){
        RoomJoinResponseDto responseDto = roomService.createRoom(requestDto);
        response.addCookie(cookieUtils.setCookie(responseDto.sessionId()));
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/join")
    public ResponseEntity<RoomJoinResponseDto> joinRoom(@Valid @RequestBody RoomJoinRequestDto roomJoinRequestDto, HttpServletResponse response){
        roomJoinRequestDto.setRole(OccupantRole.OCCUPANT);
        RoomJoinResponseDto responseDto = roomService.joinRoom(roomJoinRequestDto);
        response.addCookie(cookieUtils.setCookie(responseDto.sessionId()));
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }


    @DeleteMapping("/leave")
    @PreAuthorize("hasAnyRole('OCCUPANT', 'OWNER')")
    public ResponseEntity<Void> leaveRoom(@AuthenticationPrincipal Occupant occupant){
        roomService.leaveRoom(occupant);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OCCUPANT', 'OWNER')")
    public SseEmitter streamRoomState(@AuthenticationPrincipal Occupant occupant){
        String roomCode = occupant.getRoomCode();
        String sessionId = occupant.getSessionId().toString();

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        sseEmitters.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>())
                .put(sessionId, emitter);


        emitter.onCompletion(() -> roomEmitterHandler.removeEmitter(roomCode, sessionId));
        emitter.onTimeout(() -> roomEmitterHandler.removeEmitter(roomCode, sessionId));
        emitter.onError(e -> {
            log.error("SSE emitter error for room {} session {}: {}", roomCode, sessionId, e.getMessage());
            roomEmitterHandler.removeEmitter(roomCode, sessionId);
        });

        roomEventListener.emitRoomStateOnLogin(emitter, roomCode, sessionId);

        return emitter;
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteRoom(@AuthenticationPrincipal Occupant occupant){
        roomService.leaveRoom(occupant);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
