package com.victor.EventDrop.rooms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomEmitterHandler {
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, SseEmitter>> sseEmitters;

    public synchronized void removeEmitter(String roomCode, String sessionId) {
        ConcurrentHashMap<String, SseEmitter> sessionsInRoom = sseEmitters.get(roomCode);
        if (sessionsInRoom != null) {
            SseEmitter emitter = sessionsInRoom.get(sessionId);

            if(emitter != null) emitter.complete();
            sessionsInRoom.remove(sessionId);
            if (sessionsInRoom.isEmpty()) {
                removeRoomEmitters(roomCode);
            }

        }


    }

    public void removeRoomEmitters(String roomCode){
        sseEmitters.remove(roomCode);
    }
}
