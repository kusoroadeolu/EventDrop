package com.victor.EventDrop.orchestrators;

import com.victor.EventDrop.filedrops.FileDropService;
import com.victor.EventDrop.occupants.OccupantService;
import com.victor.EventDrop.rooms.Room;
import com.victor.EventDrop.rooms.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomStateBuilder {
    private final FileDropService fileDropService;
    private final OccupantService occupantService;
    private final RoomService roomService;

    /**
     * Builds a data transfer object (DTO) representing the current state of a room.
     * This method retrieves all relevant information—room metadata, file drops, and occupant count—
     * to provide a complete snapshot of the room's state.
     *
     * @param roomCode The room code of the room
     * @param notification The notification associated with the room state
     * @return A {@link RoomStateDto} containing the current state of the room.
     * */
    public RoomStateDto get(String roomCode, String notification){
        log.info("Getting current room state for room with room code: {}", roomCode);
        Room room = roomService.findByRoomCode(roomCode);

        return new RoomStateDto(
                room.getRoomCode(),
                room.getRoomName(),
                fileDropService.getFileDrops(roomCode),
                occupantService.getOccupantCount(roomCode),
                notification,
                room.getExpiresAt(),
                LocalDateTime.now()
        );
    }

}
