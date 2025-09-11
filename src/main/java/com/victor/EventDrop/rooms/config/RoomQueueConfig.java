package com.victor.EventDrop.rooms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomQueueConfig {

    private final RabbitAdmin rabbitAdmin;
    private final DirectExchange roomJoinExchange;
    private final RoomJoinConfigProperties roomJoinConfigProperties;

    public String declareRoomJoinQueueAndBinding(String roomCode){
        String queueName = roomJoinConfigProperties.getQueuePrefix() + roomCode;
        Queue queue = new Queue(queueName, true);
        rabbitAdmin.declareQueue(queue);

        String routingKey = roomJoinConfigProperties.getRoutingKeyPrefix() + roomCode;
        Binding binding = BindingBuilder.bind(queue).to(roomJoinExchange).with(routingKey);
        rabbitAdmin.declareBinding(binding);
        log.info("Successfully created room join queue: {}", queueName);
        return queueName;
    }

}
