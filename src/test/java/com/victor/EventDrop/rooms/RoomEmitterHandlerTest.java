package com.victor.EventDrop.rooms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomEmitterHandlerTest {


    private ConcurrentHashMap<String, ConcurrentHashMap<String, SseEmitter>> sseEmitters;
    private RoomEmitterHandler handler;

    @BeforeEach
    void setUp() {
        sseEmitters = new ConcurrentHashMap<>();
        handler = new RoomEmitterHandler(sseEmitters);
    }

    @Test
    void removeEmitter_shouldRemoveSessionFromRoom() {
        // given
        String roomCode = "room1";
        String sessionId = "session1";
        SseEmitter emitter = Mockito.mock(SseEmitter.class);

        ConcurrentHashMap<String, SseEmitter> sessions = new ConcurrentHashMap<>();
        sessions.put(sessionId, emitter);
        sseEmitters.put(roomCode, sessions);

        // when
        handler.removeEmitter(roomCode, sessionId);

        // then
        assertFalse(sseEmitters.containsKey(roomCode)); // should remove room since it’s empty
    }

    @Test
    void removeEmitter_shouldRemoveOnlyTheSessionNotRoom_ifOtherSessionsExist() {
        // given
        String roomCode = "room2";
        String session1 = "s1";
        String session2 = "s2";

        ConcurrentHashMap<String, SseEmitter> sessions = new ConcurrentHashMap<>();
        sessions.put(session1, mock(SseEmitter.class));
        sessions.put(session2, mock(SseEmitter.class));
        sseEmitters.put(roomCode, sessions);

        // when
        handler.removeEmitter(roomCode, session1);

        // then
        assertTrue(sseEmitters.containsKey(roomCode));
        assertFalse(sseEmitters.get(roomCode).containsKey(session1));
        assertTrue(sseEmitters.get(roomCode).containsKey(session2));
    }

    @Test
    void removeEmitter_shouldDoNothing_ifRoomDoesNotExist() {
        // when
        handler.removeEmitter("nonexistentRoom", "sessionX");

        // then
        assertThat(sseEmitters).isEmpty();
    }

    @Test
    void removeRoomEmitters_shouldRemoveEntireRoom() {
        // given
        String roomCode = "room3";
        sseEmitters.put(roomCode, new ConcurrentHashMap<>());

        // when
        handler.removeRoomEmitters(roomCode);

        // then
        assertFalse(sseEmitters.containsKey(roomCode));
    }
}