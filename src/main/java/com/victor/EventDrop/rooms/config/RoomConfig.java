package com.victor.EventDrop.rooms.config;

import com.victor.EventDrop.rooms.configproperties.*;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RabbitMQ configuration for room-related events.
 */
@Configuration
@RequiredArgsConstructor
public class RoomConfig {
    private final RoomJoinConfigProperties roomJoinConfigProperties;
    private final RoomExpiryConfigProperties roomExpiryConfigProperties;
    private final RoomLeaveConfigProperties roomLeaveConfigProperties;

    /**
     * Creates a SecureRandom bean for generating cryptographically strong random numbers.
     *
     * @return A SecureRandom instance.
     */
    @Bean
    public SecureRandom secureRandom(){
        return new SecureRandom();
    }

    /**
     * Creates a durable, auto-delete exchange for room join events.
     *
     * @return A DirectExchange for room join events.
     */
    @Bean
    public DirectExchange roomJoinExchange(){
        return new DirectExchange(roomJoinConfigProperties.getExchangeName(), true, false);
    }


    @Bean
    public Queue roomJoinQueue(){
        return QueueBuilder.durable(roomJoinConfigProperties.getQueueName()).quorum().build();
    }

    @Bean
    public Binding roomJoinBinding(DirectExchange roomJoinExchange, Queue roomJoinQueue){
        return BindingBuilder.bind(roomJoinQueue).to(roomJoinExchange).with(roomJoinConfigProperties.getRoutingKey());
    }

    @Bean
    public Queue roomLeaveQueue(){
        //Hardcoded string for practice
        return QueueBuilder.durable(roomLeaveConfigProperties.getQueueName()).quorum().build();
    }

    @Bean
    public Binding roomLeaveBinding(DirectExchange roomLeaveExchange, Queue roomLeaveQueue){
        return BindingBuilder.bind(roomLeaveQueue).to(roomLeaveExchange).with(roomJoinConfigProperties.getRoutingKey());
    }


    /**
     * Creates a durable exchange for room leave events.
     *
     * @return A DirectExchange for room leave events.
     */
    @Bean
    public DirectExchange roomLeaveExchange(){
        return new DirectExchange(roomLeaveConfigProperties.getExchangeName(), true, false);
    }




    /**
     * Creates a durable exchange for room expiry events.
     *
     * @return A DirectExchange for room expiry events.
     */
    @Bean
    public DirectExchange roomExpiryExchange(){
        return new DirectExchange(roomExpiryConfigProperties.getExchangeName(), true, false);
    }

    /**
     * Creates a durable queue for room expiry events.
     *
     * @return A Queue for room expiry events.
     */
    @Bean
    public Queue roomExpiryQueue(){
        return QueueBuilder.durable(roomExpiryConfigProperties.getQueueName()).quorum().build();
    }

    /**
     * Binds the room expiry queue to its exchange using a routing key.
     *
     * @param roomExpiryQueue The queue for room expiry.
     * @param roomExpiryExchange The exchange for room expiry.
     * @return A Binding connecting the queue and the exchange.
     */
    @Bean
    public Binding roomExpiryBinding(Queue roomExpiryQueue, DirectExchange roomExpiryExchange){
        return BindingBuilder.bind(roomExpiryQueue).to(roomExpiryExchange).with(roomExpiryConfigProperties.getRoutingKey());
    }

    @Bean
    public ConcurrentHashMap<String, ConcurrentHashMap<String, SseEmitter>> sseEmitters(){
        return new ConcurrentHashMap<>();
    }

}