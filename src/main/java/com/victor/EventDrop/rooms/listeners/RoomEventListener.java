package com.victor.EventDrop.rooms.listeners;

import com.victor.EventDrop.rooms.events.RoomEvent;
import com.victor.EventDrop.rooms.events.RoomEventType;
import com.victor.EventDrop.rooms.orchestrators.RoomStateBuilder;
import com.victor.EventDrop.rooms.orchestrators.RoomStateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomEventListener
{

    private final RoomStateBuilder roomStateBuilder;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, SseEmitter>> sseEmitters;
    private final AsyncTaskExecutor asyncTaskExecutor;

    /**
     * Constructs and broadcasts the current state of a room to all active SSE clients.
     * This method is triggered by a {@link RoomEvent}, retrieves the full room state,
     * and sends it to each connected {@link SseEmitter} associated with the room.
     *
     * @param roomEvent The event containing the room code and notification details.
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    public void listen(RoomEvent roomEvent){

        if(roomEvent.roomEventType() == RoomEventType.ROOM_CREATE
                || roomEvent.roomEventType() == RoomEventType.ROOM_FILE_DOWNLOAD){
            return;
        }

        RoomStateDto roomStateDto =
            roomStateBuilder.get(roomEvent.roomCode(), roomEvent.notification());

        ConcurrentHashMap<String, SseEmitter> roomEmitters = sseEmitters.get(roomEvent.roomCode());
        if (roomEmitters != null){
            roomEmitters.forEach((sessionId, emitter) -> {
                asyncTaskExecutor.execute(() -> {
                    try {
                        emitter.send(roomStateDto);
                        log.info("Sent message to session {} in room {}", sessionId, roomEvent.roomCode());
                    } catch (IOException e) {
                        log.error("Failed to send room state to session {} in room {}: {}", sessionId, roomEvent.roomCode(), e.getMessage());
                        emitter.completeWithError(e);
                    }
                });
            });
        }else{
            log.info("No active sessions found for room: {}", roomEvent.roomCode());
        }
    }

    public void emitRoomStateOnLogin(SseEmitter emitter, String roomCode, String sessionId){
        RoomStateDto roomStateDto =
                roomStateBuilder.get(roomCode, null);

        asyncTaskExecutor.execute(() -> {
            try{
                emitter.send(roomStateDto);
                log.info("Sent initial state to session {} in room {}", sessionId, roomCode);
            }catch (IOException e){
                log.error("Failed to send room state on login to session {} in room {}: {}", sessionId, roomCode, e.getMessage());
                emitter.completeWithError(e);
            }
        });
    }
}
