package com.victor.EventDrop.rooms;

import com.victor.EventDrop.exceptions.*;
import com.victor.EventDrop.occupants.Occupant;
import com.victor.EventDrop.occupants.OccupantRole;
import com.victor.EventDrop.occupants.OccupantRoomJoinResponse;
import com.victor.EventDrop.rooms.configproperties.RoomJoinConfigProperties;
import com.victor.EventDrop.rooms.configproperties.RoomLeaveConfigProperties;
import com.victor.EventDrop.rooms.dtos.*;
import com.victor.EventDrop.rooms.events.RoomEvent;
import com.victor.EventDrop.rooms.events.RoomEventType;
import com.victor.EventDrop.rooms.events.RoomJoinEvent;
import com.victor.EventDrop.rooms.events.RoomLeaveEvent;
import com.victor.EventDrop.rooms.listeners.RoomQueueListenerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
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

    private static final int MAX_ROOM_CODE_CREATION_ATTEMPTS = 5;

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
        double ttlInMinutes = roomCreateRequestDto.ttl();
        //TTL given is in minutes. Convert it to seconds
        double ttlInSeconds = ttlInMinutes * 60.0;

        String roomCode = validateTtlAndGenerateRoomCode(ttlInMinutes);

        try{
            LocalDateTime createdAt = LocalDateTime.now();
            Room room = Room
                    .builder()
                    .roomCode(roomCode)
                    .roomName(roomCreateRequestDto.roomName().trim())
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


    protected String validateTtlAndGenerateRoomCode(double ttl){
        //Cant create rooms that last more than 3 days
        if (ttl > maxTtlInMins) {
            throw new RoomTtlExceededException(
                    String.format("Room TTL of %d minutes exceeded the maximum of %d minutes.", (long) ttl, maxTtlInMins)
            );
        }

        return ensureUniqueRoomCode();
    }

    protected String ensureUniqueRoomCode(){
        String roomCode = generateRoomCode();
        int currentAttempts = 1;

        while(roomRepository.existsByRoomCode(roomCode) && currentAttempts <= MAX_ROOM_CODE_CREATION_ATTEMPTS){
            if (currentAttempts == MAX_ROOM_CODE_CREATION_ATTEMPTS) {
                log.info("Failed to generate unique room code after "
                        + MAX_ROOM_CODE_CREATION_ATTEMPTS + " attempts");
                throw new RoomCreationException("Failed to generate unique room code after "
                        + MAX_ROOM_CODE_CREATION_ATTEMPTS + " attempts");
            }

            log.info("Found existing room with room code: {}. Regenerating room code...", roomCode);
            roomCode = generateRoomCode();
            currentAttempts++;
        }

        return roomCode;
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
        String roomCode = roomJoinRequestDto.getRoomCode().trim();
        String username = roomJoinRequestDto.getUsername().trim();
        log.info("Attempting to join room: {}", roomCode);

        Room room = findByRoomCode(roomCode);

        log.info("Found room with room code: {}. Joining... ", roomCode);
        UUID sessionId = UUID.randomUUID();
        String routingKey = roomJoinConfigProperties.getRoutingKeyPrefix() + roomCode;

        //Sends a blocking room creation event to create an occupant. Expects a room join response
        OccupantRoomJoinResponse roomJoinResponse = (OccupantRoomJoinResponse) rabbitTemplate.convertSendAndReceive(
                roomJoinConfigProperties.getExchangeName(),
                routingKey,
                new RoomJoinEvent(username, sessionId ,roomJoinRequestDto.getRole() ,roomJoinRequestDto.getRoomCode(), room.getExpiresAt())
        );

        handleRoomJoinResponse(roomJoinResponse);

        eventPublisher.publishEvent(
                new RoomEvent(
                        username + " joined the room",
                        LocalDateTime.now(),
                        RoomEventType.ROOM_JOIN,
                        roomCode,
                        null
                )
        );


        return roomMapper.toRoomJoinResponseDto(room, sessionId.toString(), username);
    }

    //Handles the room join response from the listener container
    private void handleRoomJoinResponse(OccupantRoomJoinResponse roomJoinResponse) {
        if(roomJoinResponse == null){
            log.info("Failed to join room because occupant room join response is null");
            throw new RoomJoinException("Failed to join room because occupant room join response is null");
        }

        if(roomJoinResponse.success()){
            log.info("Successfully joined room");
        }else{
            switch (roomJoinResponse.status()){
                case 409 -> {
                    log.info("Cannot join room because room is full");
                    throw new RoomFullException("Cannot join room because room is full");
                }
                case 500 -> {
                    log.info("An unexpected error occurred while trying to join this room");
                    throw new RoomJoinException("An unexpected error occurred while trying to join this room");
                }
            }
        }
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

        rabbitTemplate.convertAndSend(
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
     * Finds a room by its unique room code.
     *
     * @param roomCode The room code to search for.
     * @return The {@link Room} entity if found.
     * @throws NoSuchRoomException if no room is found with the given room code.
     */
    @Override
    public Optional<Room> findOptionalRoomByRoomCode(String roomCode){
        return roomRepository
                .findByRoomCode(roomCode);
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