package com.victor.EventDrop.rooms;

import com.victor.EventDrop.rooms.configproperties.RoomJoinConfigProperties;
import com.victor.EventDrop.rooms.configproperties.RoomLeaveConfigProperties;
import com.victor.EventDrop.rooms.dtos.RoomQueueDeclareDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

/**
 * Service for dynamically declaring RabbitMQ queues and bindings for rooms.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomQueueDeclarationServiceImpl implements RoomQueueDeclarationService {

    private final RabbitAdmin rabbitAdmin;
    private final DirectExchange roomJoinExchange;
    private final DirectExchange roomLeaveExchange;
    private final RoomJoinConfigProperties roomJoinConfigProperties;
    private final RoomLeaveConfigProperties roomLeaveConfigProperties;

    /**
     * Declares a durable queue and binds it to a specified exchange.
     *
     * @param declareDto DTO containing queue and routing key prefixes and the room code.
     * @param exchange The exchange to bind the new queue to.
     * @return The name of the declared queue.
     */
    public String declareRoomQueueAndBinding(RoomQueueDeclareDto declareDto, DirectExchange exchange){
        String queueName = declareDto.queuePrefix() + declareDto.roomCode();
        Queue queue = new Queue(queueName, true);
        rabbitAdmin.declareQueue(queue);

        String routingKey = declareDto.routingKeyPrefix() + declareDto.roomCode();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        rabbitAdmin.declareBinding(binding);
        log.info("Successfully started room queue: {}", queueName);
        return queueName;
    }

    /**
     * Declares a queue and binding for room join events.
     *
     * @param roomCode The unique code for the room.
     * @return The name of the declared queue.
     */
    @Override
    public String declareRoomJoinQueueAndBinding(String roomCode){
        var queueDeclareDto = new RoomQueueDeclareDto(roomJoinConfigProperties.getQueuePrefix(), roomJoinConfigProperties.getRoutingKeyPrefix(), roomCode);
        return declareRoomQueueAndBinding(queueDeclareDto, roomJoinExchange);
    }

    /**
     * Declares a queue and binding for room leave events.
     *
     * @param roomCode The unique code for the room.
     * @return The name of the declared queue.
     */
    @Override
    public String declareRoomLeaveQueueAndBinding(String roomCode){
        var queueDeclareDto = new RoomQueueDeclareDto(roomLeaveConfigProperties.getQueuePrefix(), roomLeaveConfigProperties.getRoutingKeyPrefix(), roomCode);
        return declareRoomQueueAndBinding(queueDeclareDto, roomLeaveExchange);
    }

    @Override
    public void deleteAllQueues(String roomCode){
        try{
            rabbitAdmin.deleteQueue(roomJoinConfigProperties.getQueuePrefix() + roomCode);
            rabbitAdmin.deleteQueue(roomLeaveConfigProperties.getQueuePrefix() + roomCode);
        }catch (Exception e){
            log.info("Failed to delete all queues for room with room code: {}", roomCode);
        }
    }
}