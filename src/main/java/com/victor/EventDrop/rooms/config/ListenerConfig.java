package com.victor.EventDrop.rooms.config;


import com.victor.EventDrop.occupants.OccupantService;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for various application beans.
 */
@Configuration
public class ListenerConfig {

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
     * Creates a thread-safe map to store listeners for room join events.
     *
     * @return A ConcurrentHashMap to manage SimpleMessageListenerContainer beans.
     */
    @Bean("roomJoinListenersMap")
    public ConcurrentHashMap<String, SimpleMessageListenerContainer> roomJoinListenersMap(){
        return new ConcurrentHashMap<>();
    }
    /**
     * Creates a thread-safe map to store listeners for room leave events.
     *
     * @return A ConcurrentHashMap to manage SimpleMessageListenerContainer beans.
     */
    @Bean("roomLeaveListenersMap")
    public ConcurrentHashMap<String, SimpleMessageListenerContainer> roomLeaveListenersMap(){
        return new ConcurrentHashMap<>();
    }

    /**
     * Adapts incoming messages to the "createOccupant" method of OccupantService.
     *
     * @param occupantService The service to handle the message.
     * @param messageConverter The converter to deserialize the message payload.
     * @return A MessageListenerAdapter for room join events.
     */
    @Bean("roomJoinAdapter")
    public MessageListenerAdapter roomJoinAdapter(OccupantService occupantService, Jackson2JsonMessageConverter messageConverter){
        MessageListenerAdapter adapter = new MessageListenerAdapter(occupantService, "createOccupant");
        adapter.setMessageConverter(messageConverter);
        return adapter;
    }


    /**
     * Creates a listener adapter for room leave events.
     *
     * @param messageConverter The converter to deserialize the message payload.
     * @return A MessageListenerAdapter for room leave events.
     */
    @Bean("roomLeaveAdapter")
    public MessageListenerAdapter roomLeaveAdapter(OccupantService occupantService,  Jackson2JsonMessageConverter messageConverter){
        MessageListenerAdapter adapter = new MessageListenerAdapter(occupantService, "deleteOccupant");
        adapter.setMessageConverter(messageConverter);
        return adapter;
    }

    @Bean
    public ConcurrentHashMap<String, ConcurrentHashMap<String, SseEmitter>> sseEmitters(){
        return new ConcurrentHashMap<>();
    }
}