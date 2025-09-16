package com.victor.EventDrop.orchestrators;

import com.victor.EventDrop.rooms.events.RoomEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomEventListener
{

    private final RoomStateBuilder roomStateBuilder;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, SseEmitter>> sseEmitters;
    private final ExecutorService executorService;

    /**
     * Constructs and broadcasts the current state of a room to all active SSE clients.
     * This method is triggered by a {@link RoomEvent}, retrieves the full room state,
     * and sends it to each connected {@link SseEmitter} associated with the room.
     *
     * @param roomEvent The event containing the room code and notification details.
     */
    @EventListener
    public void listen(RoomEvent roomEvent){
        RoomStateDto roomStateDto =
                roomStateBuilder.get(roomEvent.roomCode(), roomEvent.notification());

        ConcurrentHashMap<String, SseEmitter> roomEmitters = sseEmitters.get(roomEvent.roomCode());
        if (roomEmitters != null){
            roomEmitters.forEach((sessionId, emitter) -> {
                executorService.execute(() -> {
                    try {
                        emitter.send(roomStateDto);
                        log.debug("Sent message to session {} in room {}", sessionId, roomEvent.roomCode());
                    } catch (IOException e) {
                        log.error("Failed to send to session {} in room {}: {}", sessionId, roomEvent.roomCode(), e.getMessage());
                        emitter.completeWithError(e);
                    }
                });
            });
        }else{
            log.info("No active sessions found for room: {}", roomEvent.roomCode());
        }
    }
}
