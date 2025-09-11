package com.victor.EventDrop.occupants;

import com.victor.EventDrop.exceptions.OccupantCreationException;
import com.victor.EventDrop.rooms.dtos.RoomJoinEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class OccupantServiceImpl {

    private final OccupantRepository occupantRepository;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "${room.join.queue-name}")
    public void createOccupant(RoomJoinEvent roomJoinEvent){
        log.info("Initiating room occupant creation for room: {}", roomJoinEvent.roomCode());
        Occupant occupant = Occupant
                .builder()
                .occupantName(roomJoinEvent.username())
                .roomCode(roomJoinEvent.roomCode())
                .occupantRole(roomJoinEvent.role())
                .sessionId(roomJoinEvent.sessionId())
                .build();

        try {
            log.info("Attempting to save occupant: {}", occupant.getOccupantName());
            occupantRepository.save(occupant);
            log.info("Successfully saved occupant: {}", occupant.getOccupantName());
        }catch (Exception e){
            log.info("An unexpected error occurred while trying to save occupant: {}", occupant.getOccupantName(), e);
            throw new OccupantCreationException(String.format("An unexpected error occurred while trying to save occupant: %s", occupant.getOccupantName()), e);
        }

    }
}
