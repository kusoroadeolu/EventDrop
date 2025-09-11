package com.victor.EventDrop.rooms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RoomConfig {
    private final RoomJoinConfigProperties roomJoinConfigProperties;

    @Bean
    public Queue roomCreationQueue(){
        return new Queue(roomJoinConfigProperties.getQueueName(), true);
    }

    @Bean
    public DirectExchange roomCreationExchange(){
        return new DirectExchange(roomJoinConfigProperties.getExchangeName(), true, true);
    }

    @Bean
    public Binding roomCreationBinding(Queue roomCreationQueue, DirectExchange roomCreationExchange){
        return BindingBuilder.bind(roomCreationQueue).to(roomCreationExchange).with(roomJoinConfigProperties.getRoutingKey());
    }
}
