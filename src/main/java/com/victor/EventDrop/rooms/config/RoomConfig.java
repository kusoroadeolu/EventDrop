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
    private final RoomExpiryConfigProperties roomExpiryConfigProperties;

    @Bean
    public DirectExchange roomJoinExchange(){
        return new DirectExchange(roomJoinConfigProperties.getExchangeName(), true, true);
    }

    @Bean
    public DirectExchange roomExpiryExchange(){
        return new DirectExchange(roomExpiryConfigProperties.getExchangeName(), true, false);
    }

    @Bean
    public Queue roomExpiryQueue(){
        return new Queue(roomExpiryConfigProperties.getQueueName(), true);
    }

    @Bean
    public Binding roomExpiryBinding(Queue roomExpiryQueue, DirectExchange roomExpiryExchange){
        return BindingBuilder.bind(roomExpiryQueue).to(roomExpiryExchange).with(roomExpiryConfigProperties.getRoutingKey());
    }



}
