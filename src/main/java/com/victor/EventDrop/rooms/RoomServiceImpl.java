package com.victor.EventDrop.rooms;

import com.victor.EventDrop.exceptions.NoSuchRoomException;
import com.victor.EventDrop.exceptions.RoomCreationException;
import com.victor.EventDrop.occupants.OccupantRole;
import com.victor.EventDrop.rabbitmq.RoomJoinListenerService;
import com.victor.EventDrop.rooms.config.RoomJoinConfigProperties;
import com.victor.EventDrop.rooms.config.RoomQueueConfig;
import com.victor.EventDrop.rooms.dtos.*;
import com.victor.EventDrop.rooms.events.RoomJoinEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomJoinConfigProperties roomJoinConfigProperties;
    private final RoomRepository roomRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RoomQueueConfig roomQueueConfig;
    private final RoomMapper roomMapper;
    private final SecureRandom secureRandom;
    private final RoomJoinListenerService roomJoinListenerService;


    //Orchestrates the creation of a room
    @Override
    public RoomJoinResponseDto createRoom(RoomCreateRequestDto roomCreateRequestDto){
        log.info("Initiating room creation");
        String roomCode = generateRoomCode();

        //TTL given is in minutes. Convert it to seconds
        long ttlInSeconds = roomCreateRequestDto.ttl() * 60;

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

            String queueName = roomQueueConfig.declareRoomJoinQueueAndBinding(roomCode);
            roomJoinListenerService.startListeners(queueName);

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
        
        Optional<Room> optionalRoom = roomRepository.findByRoomCode(roomCode);

        if(optionalRoom.isEmpty()){
            log.info("Failed to find room with room code: {}", roomCode);
            throw new NoSuchRoomException(String.format("Failed to find room with room code: %s", roomCode));
        }

        Room room = optionalRoom.get();
        log.info("Found room with room code: {}. Joining... ", roomCode);

        UUID sessionId = UUID.randomUUID();
        String routingKey = roomJoinConfigProperties.getRoutingKeyPrefix() + roomCode;

        //Sends a room creation event to create an occupant
        rabbitTemplate.convertAndSend(
                roomJoinConfigProperties.getExchangeName(),
                routingKey,
                new RoomJoinEvent(roomJoinRequestDto.getUsername(), sessionId ,roomJoinRequestDto.getRole() ,roomJoinRequestDto.getRoomCode())
        );

        return roomMapper.toRoomJoinResponseDto(room, sessionId.toString(), roomJoinRequestDto.getUsername());
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
    public void stopAllListeners(String roomCode){
        roomJoinListenerService.stopListener(roomJoinConfigProperties.getQueuePrefix() + roomCode);

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
