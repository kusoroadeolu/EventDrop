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
import com.victor.EventDrop.rooms.events.RoomJoinEvent;
import com.victor.EventDrop.rooms.events.RoomLeaveEvent;
import com.victor.EventDrop.rooms.listeners.RoomQueueListenerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

    @Value("${room.max-ttl-in-minutes}")
    private long maxTtlInMins;

    //Orchestrates the creation of a room
    @Override
    public RoomJoinResponseDto createRoom(RoomCreateRequestDto roomCreateRequestDto){
        log.info("Initiating room creation");
        String roomCode = generateRoomCode();

        while(roomRepository.existsByRoomCode(roomCode)){
            log.info("Found existing room with room code: {}. Regenerating room code...", roomCode);
            roomCode = generateRoomCode();
        }

        //TTL given is in minutes. Convert it to seconds
        long ttlInSeconds = roomCreateRequestDto.ttl() * 60;

        //Cant create rooms that last more than 3 days
        if(ttlInSeconds > maxTtlInMins){
            log.info("Cannot create room with room-code {} because its max TTL was exceeded.", roomCode);
            throw new RoomTtlExceededException(
                    String.format("Room TTL of %d seconds exceeded the maximum of %d minutes.", ttlInSeconds, maxTtlInMins)
            );
        }

        try{
            LocalDateTime createdAt = LocalDateTime.now();
            Room room = Room
                    .builder()
                    .roomCode(roomCode)
                    .roomName(roomCreateRequestDto.roomName())
                    .ttl(ttlInSeconds)
                    .createdAt(createdAt)
                    .expiresAt(createdAt.plusMinutes(roomCreateRequestDto.ttl()))
                    .build();

            roomRepository.save(room);
            roomQueueListenerService.startListeners(roomCode);
            log.info("Successfully created room: {} with room code: {}", room.getRoomName(), room.getRoomCode());

            return joinRoom(new RoomJoinRequestDto(roomCreateRequestDto.username(), OccupantRole.OWNER ,roomCode));

        }catch (Exception e){
            log.info("Failed to create room with room-code: {}", roomCode, e);
            throw new RoomCreationException(String.format("Failed to create room with room-code: %s", roomCode), e);
        }
    }

    //Orchestrates room joins
    @Override
    public RoomJoinResponseDto joinRoom(RoomJoinRequestDto roomJoinRequestDto){
        String roomCode = roomJoinRequestDto.getRoomCode();
        log.info("Attempting to join room: {}", roomCode);

        Room room = roomRepository
                .findByRoomCode(roomCode)
                .orElseThrow(() -> new NoSuchRoomException(String.format("Failed to find room with room code: %s", roomCode)));

        log.info("Found room with room code: {}. Joining... ", roomCode);
        UUID sessionId = UUID.randomUUID();
        String routingKey = roomJoinConfigProperties.getRoutingKeyPrefix() + roomCode;

        //Sends a room creation event to create an occupant
        rabbitTemplate.convertAndSend(
                roomJoinConfigProperties.getExchangeName(),
                routingKey,
                new RoomJoinEvent(roomJoinRequestDto.getUsername(), sessionId ,roomJoinRequestDto.getRole() ,roomJoinRequestDto.getRoomCode(), room.getExpiresAt())
        );

        return roomMapper.toRoomJoinResponseDto(room, sessionId.toString(), roomJoinRequestDto.getUsername());
    }

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
    }

    @Override
    public void deleteRoom(Occupant occupant){
        leaveRoom(occupant);
        deleteByRoomCode(occupant.getRoomCode());
    }

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


    //Generates a unique alphanumeric 8 digit room code
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
