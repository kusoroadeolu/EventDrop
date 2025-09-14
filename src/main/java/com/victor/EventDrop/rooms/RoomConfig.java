package com.victor.EventDrop.rooms;

import com.victor.EventDrop.rooms.configproperties.*;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for room-related events.
 */
@Configuration
@RequiredArgsConstructor
public class RoomConfig {
    private final RoomJoinConfigProperties roomJoinConfigProperties;
    private final RoomFileUploadConfigProperties roomFileUploadConfigProperties;
    private final RoomExpiryConfigProperties roomExpiryConfigProperties;
    private final RoomFileDeleteConfigProperties roomFileDeleteConfigProperties;
    private final RoomLeaveConfigProperties roomLeaveConfigProperties;

    /**
     * Creates a durable, auto-delete exchange for room join events.
     *
     * @return A DirectExchange for room join events.
     */
    @Bean
    public DirectExchange roomJoinExchange(){
        return new DirectExchange(roomJoinConfigProperties.getExchangeName(), true, true);
    }

    /**
     * Creates a durable exchange for file upload events.
     *
     * @return A DirectExchange for file upload events.
     */
    @Bean
    public DirectExchange roomFileUploadExchange(){
        return new DirectExchange(roomFileUploadConfigProperties.getExchangeName(), true, false);
    }

    /**
     * Creates a durable exchange for file deletion events.
     *
     * @return A DirectExchange for file deletion events.
     */
    @Bean
    public DirectExchange roomFileDeleteExchange(){
        return new DirectExchange(roomFileDeleteConfigProperties.getExchangeName(), true, false);
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
        return new Queue(roomExpiryConfigProperties.getQueueName(), true);
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


}