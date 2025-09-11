package com.victor.EventDrop;

import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class MiscConfig {
    @Bean
    public SecureRandom secureRandom(){
        return new SecureRandom();
    }

    public Map<String, SimpleMessageListenerContainer> roomJoinListenersMap(){
        return new ConcurrentHashMap<>();
    }
}
