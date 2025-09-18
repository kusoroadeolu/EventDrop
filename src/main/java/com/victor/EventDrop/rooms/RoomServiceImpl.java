package com.victor.EventDrop.rooms;

import com.victor.EventDrop.exceptions.NoSuchRoomException;
import com.victor.EventDrop.exceptions.RoomCreationException;
import com.victor.EventDrop.exceptions.RoomDeletionException;
import com.victor.EventDrop.exceptions.RoomTtlExceededException;
import com.victor.EventDrop.occupants.Occupant;
import com.victor.EventDrop.occupants.OccupantRole;
import com.victor.EventDrop.rooms.configproperties.RoomJoinConfigProperties;
import com.victor.EventDrop.rooms.configproperties.RoomLeaveConfigProperties;
import com.victor.EventDrop.rooms.dtos.*;
import com.victor.EventDrop.rooms.events.RoomEvent;
import com.victor.EventDrop.rooms.events.RoomEventType;
import com.victor.EventDrop.rooms.events.RoomJoinEvent;
import com.victor.EventDrop.rooms.events.RoomLeaveEvent;
import com.victor.EventDrop.rooms.listeners.RoomQueueListenerService;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service class for managing room-related operations.
 * This service handles room creation, joining, leaving, and deletion,
 * coordinating with Redis, RabbitMQ, and event listeners.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomJoinConfigProperties roomJoinConfigProperties;
    private final RoomLeaveConfigProperties roomLeaveConfigProperties;
    private final RoomRepository roomRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RoomMapper roomMapper;
    private final SecureRandom secureRandom;
    private final RoomQueueListenerService roomQueueListenerService;
    private final ApplicationEventPublisher eventPublisher;
    @Value("${room.max-ttl-in-minutes}")
    private long maxTtlInMins;

    /**
     * Orchestrates the creation of a new room.
     * It generates a unique room code, validates the TTL, saves the room to the repository,
     * and initializes the room's message queue listeners.
     *
     * @param roomCreateRequestDto The DTO containing the room's name and TTL.
     * @return A {@link RoomJoinResponseDto} representing the initial state after the owner joins the room.
     * @throws RoomTtlExceededException if the requested TTL exceeds the configured maximum.
     * @throws RoomCreationException    if the room creation process fails.
     */
    @Override
    public RoomJoinResponseDto createRoom(RoomCreateRequestDto roomCreateRequestDto){
        log.info("Initiating room creation");
        String roomCode = generateRoomCode();

        while(roomRepository.existsByRoomCode(roomCode)){
            log.info("Found existing room with room code: {}. Regenerating room code...", roomCode);
            roomCode = generateRoomCode();
        }

        //Cant create rooms that last more than 3 days
        if(roomCreateRequestDto.ttl() > maxTtlInMins){
            log.info("Cannot create room with room-code {} because its max TTL was exceeded.", roomCode);
            throw new RoomTtlExceededException(
                    String.format("Room TTL of %d minutes exceeded the maximum of %d minutes.", (long)roomCreateRequestDto.ttl(), maxTtlInMins)
            );
        }

        //TTL given is in minutes. Convert it to seconds
        double ttlInSeconds = roomCreateRequestDto.ttl() * 60.0;

        try{
            LocalDateTime createdAt = LocalDateTime.now();
            Room room = Room
                    .builder()
                    .roomCode(roomCode)
                    .roomName(roomCreateRequestDto.roomName())
                    .ttl(ttlInSeconds)
                    .createdAt(createdAt)
                    .expiresAt(createdAt.plusSeconds((long) ttlInSeconds))
                    .build();

            roomRepository.save(room);
            roomQueueListenerService.startListeners(roomCode);

            eventPublisher.publishEvent(
                    new RoomEvent(
                            roomCreateRequestDto.username() + " created the room",
                            LocalDateTime.now(),
                            RoomEventType.ROOM_CREATE,
                            roomCode,
                            null
                    )
            );

            log.info("Successfully created room: {} with room code: {}", room.getRoomName(), room.getRoomCode());
            return joinRoom(new RoomJoinRequestDto(roomCreateRequestDto.username(), OccupantRole.OWNER ,roomCode));

        }catch (Exception e){
            log.info("Failed to create room with room-code: {}", roomCode, e);
            throw new RoomCreationException(String.format("Failed to create room with room-code: %s", roomCode), e);
        }
    }

    /**
     * Orchestrates the process of an occupant joining a room.
     * A new session ID is generated, and a message is sent to RabbitMQ to trigger
     * occupant creation. An SSE event is then published to notify clients.
     *
     * @param roomJoinRequestDto The DTO containing the room code, username, and role.
     * @return A {@link RoomJoinResponseDto} with the new session ID and room details.
     * @throws NoSuchRoomException if the specified room does not exist.
     */
    @Override
    public RoomJoinResponseDto joinRoom(RoomJoinRequestDto roomJoinRequestDto){
        String roomCode = roomJoinRequestDto.getRoomCode();
        log.info("Attempting to join room: {}", roomCode);

        Room room = findByRoomCode(roomCode);

        log.info("Found room with room code: {}. Joining... ", roomCode);
        UUID sessionId = UUID.randomUUID();
        String routingKey = roomJoinConfigProperties.getRoutingKeyPrefix() + roomCode;

        //Sends a room creation event to create an occupant
        String occupantName = (String) rabbitTemplate.convertSendAndReceive(
                roomJoinConfigProperties.getExchangeName(),
                routingKey,
                new RoomJoinEvent(roomJoinRequestDto.getUsername(), sessionId ,roomJoinRequestDto.getRole() ,roomJoinRequestDto.getRoomCode(), room.getExpiresAt())
        );

        //Publish the event to the sse listener
        eventPublisher.publishEvent(
                new RoomEvent(
                        occupantName + " joined the room",
                        LocalDateTime.now(),
                        RoomEventType.ROOM_JOIN,
                        roomCode,
                        null
                )
        );

        return roomMapper.toRoomJoinResponseDto(room, sessionId.toString(), roomJoinRequestDto.getUsername());
    }

    /**
     * Handles the event of an occupant leaving a room.
     * A message is sent to RabbitMQ to trigger cleanup of the occupant's session.
     * An SSE event is also published to notify clients.
     *
     * @param occupant The {@link Occupant} object of the individual leaving the room.
     */
    @Override
    public void leaveRoom(Occupant occupant){
        log.info("Handling room leave for occupant with ID: {}", occupant.getSessionId());
        String roomCode = occupant.getRoomCode();
        String routingKey = roomLeaveConfigProperties.getRoutingKeyPrefix() + roomCode;

        Boolean result = (Boolean) rabbitTemplate.convertSendAndReceive(
                roomLeaveConfigProperties.getExchangeName(),
                routingKey,
                new RoomLeaveEvent(
                        roomCode, occupant.getOccupantName(), occupant.getSessionId()
                )
        );

        eventPublisher.publishEvent(
                new RoomEvent(
                        occupant.getOccupantName() + " left the room",
                        LocalDateTime.now(),
                        RoomEventType.ROOM_LEAVE,
                        roomCode,
                        null
                )
        );
    }

    /**
     * Orchestrates the deletion of a room by first triggering a leave event for the owner
     * and then deleting the room from the repository.
     *
     * @param occupant The {@link Occupant} object representing the room's owner.
     */
    @Override
    public void deleteRoom(Occupant occupant){
        leaveRoom(occupant);
        deleteByRoomCode(occupant.getRoomCode());
    }

    /**
     * Retrieves a list of all active rooms and their metadata.
     *
     * @return A list of {@link RoomResponseDto} containing details of active rooms.
     */
    @Override
    public List<RoomResponseDto> findAllActiveRooms(){
        List<Room> rooms = (List<Room>) roomRepository.findAll();
        return rooms.stream()
                .filter(Objects::nonNull)
                .map(room -> new RoomResponseDto(
                        room.getRoomCode(),
                        room.getRoomName(),
                        Duration.between(LocalDateTime.now(), room.getExpiresAt()).toMinutes()
                )).toList();

    }

    /**
     * Finds a room by its unique room code.
     *
     * @param roomCode The room code to search for.
     * @return The {@link Room} entity if found.
     * @throws NoSuchRoomException if no room is found with the given room code.
     */
    public Room findByRoomCode(String roomCode){
        return roomRepository
                .findByRoomCode(roomCode)
                .orElseThrow(() -> new NoSuchRoomException(String.format("Failed to find room with room code: %s", roomCode)));
    }

    /**
     * Deletes a room from the repository by its room code.
     *
     * @param roomCode The room code of the room to be deleted.
     * @throws RoomDeletionException if the room cannot be deleted from the repository.
     */
    @Override
    public void deleteByRoomCode(String roomCode){
        try{
            roomRepository.deleteById(roomCode);
            log.info("Successfully deleted room with room code: {}", roomCode);
        }catch (Exception e){
            log.info("Failed to delete room with room code: {}", roomCode);
            throw new RoomDeletionException(String.format("Failed to delete room with room code: %s", roomCode));
        }
    }


    /**
     * Generates a unique, 8-character alphanumeric room code.
     *
     * @return An 8-character unique room code.
     */
    private String generateRoomCode(){
        String vars = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        int len = vars.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++){
            int random = secureRandom.nextInt(len);
            sb.append(vars.charAt(random));
        }

        log.info("Generated room code: {}", sb);
        return sb.toString();
    }

}