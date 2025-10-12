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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomEventListener
{

    private final RoomStateBuilder roomStateBuilder;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, SseEmitter>> sseEmitters;
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<RoomStateDto>> roomEventHashMap;
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

        //Just ignore room create or download events, these are used for analytics
        if(roomEvent.roomEventType() == RoomEventType.ROOM_CREATE
                || roomEvent.roomEventType() == RoomEventType.ROOM_FILE_DOWNLOAD){
            return;
        }

        String roomCode = roomEvent.roomCode();

        RoomStateDto roomStateDto =
            roomStateBuilder.get(roomCode, roomEvent.notification());


        //Map each room to their own queue if it doesnt exist
        roomEventHashMap.computeIfAbsent(roomCode, k -> new ConcurrentLinkedDeque<>());

        // This queue streams events synchronously but doesn't block the main thread
        ConcurrentLinkedDeque<RoomStateDto> roomStateDtos = roomEventHashMap.get(roomCode);

        // Add events to the queue based on their importance
        switch (roomEvent.roomEventType()){
            case ROOM_EXPIRY -> roomStateDtos.addFirst(roomStateDto);
            case ROOM_LEAVE -> roomStateDtos.addLast(roomStateDto);
            default -> roomStateDtos.add(roomStateDto);
        }

        processQueueForRoom(roomCode);
    }


    /**
     * This process events for a room through a thread safe queue {@link ConcurrentLinkedDeque}.
     * @param roomCode The roomCode of the room
     * */
    private void processQueueForRoom(String roomCode){
        Map<String, SseEmitter> roomEmitters = sseEmitters.get(roomCode);

        //The queue which handles the sequential room state processing logic of that room
        Queue<RoomStateDto> roomStateDtos = roomEventHashMap.get(roomCode);
        if(roomStateDtos == null)return;

        //Ensure only one thread can emit to the room at all times to prevent race conditions
        synchronized (roomStateDtos){
            RoomStateDto queuedDto;
            while ((queuedDto = roomStateDtos.poll()) != null){
                if (roomEmitters != null){
                    streamRoomEventToRoom(roomEmitters, queuedDto);
                }else{
                    log.info("No active sessions found for room: {}", roomCode);
                }
            }

        }
    }

    //This method sequentially streams room events to all members of a room
    private void streamRoomEventToRoom(Map<String, SseEmitter> roomEmitters, RoomStateDto finalQueuedDto){
        String roomCode = finalQueuedDto.roomCode();
        roomEmitters.forEach((sessionId, emitter) -> {
            try {
                emitter.send(finalQueuedDto);
                log.info("Sent message to session {} in room {}", sessionId, roomCode);
            } catch (IOException e) {
                log.error("Failed to send room state to session {} in room {}: {}", sessionId, roomCode, e.getMessage(), e);
                emitter.completeWithError(e);
            } catch (AccessDeniedException e){
                log.error("Authorization error occurred during streaming. Terminating gracefully...", e);
                emitter.completeWithError(e);
            }catch (Exception e){
                log.error("Unexpected error occurred during streaming. Terminating gracefully...", e);
                emitter.completeWithError(e);
            }
        });
    }

    //Immediately emits the room state for a user on login/room join
    public void emitRoomStateOnRoomJoin(SseEmitter emitter, String roomCode, String sessionId){
        RoomStateDto roomStateDto =
                roomStateBuilder.get(roomCode, null);

        asyncTaskExecutor.execute(() -> {
            try{
                emitter.send(roomStateDto);
                log.info("Sent initial state to session {} in room {}", sessionId, roomCode);
            }catch (IOException e){
                log.error("Failed to send room state on login to session {} in room {}: {}", sessionId, roomCode, e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });
    }
}
