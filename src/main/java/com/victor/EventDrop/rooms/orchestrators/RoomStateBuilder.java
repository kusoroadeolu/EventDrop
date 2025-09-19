package com.victor.EventDrop.rooms.orchestrators;

import com.victor.EventDrop.filedrops.FileDropService;
import com.victor.EventDrop.occupants.OccupantService;
import com.victor.EventDrop.rooms.Room;
import com.victor.EventDrop.rooms.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        Optional<Room> optionalRoom = roomService.findOptionalRoomByRoomCode(roomCode);

        if (optionalRoom.isEmpty()){
            return new RoomStateDto(
                    "",
                    "",
                    List.of(),
                    0,
                    "This room has expired.",
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    true
            );
        }

        Room room = optionalRoom.get();

        return new RoomStateDto(
                room.getRoomCode(),
                room.getRoomName(),
                fileDropService.getFileDrops(roomCode),
                occupantService.getOccupantCount(roomCode),
                notification,
                room.getExpiresAt(),
                LocalDateTime.now(),
                false
        );
    }

}
