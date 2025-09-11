package com.victor.EventDrop.rooms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@RequiredArgsConstructor
public class RoomConfig {
    private final RoomJoinConfigProperties roomJoinConfigProperties;

    @Bean
    public DirectExchange roomJoinExchange(){
        return new DirectExchange(roomJoinConfigProperties.getExchangeName(), true, true);
    }



}
