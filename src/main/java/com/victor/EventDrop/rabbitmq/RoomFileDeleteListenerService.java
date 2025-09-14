package com.victor.EventDrop.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RoomFileDeleteListenerService extends AbstractQueueListenerService{

    private final ConcurrentHashMap<String, SimpleMessageListenerContainer> roomFileDeleteListenersMap;
    private final MessageListenerAdapter roomFileDeleteAdapter;

    public RoomFileDeleteListenerService(CachingConnectionFactory connectionFactory,
                                         @Qualifier("roomFileDeleteListenersMap")
                                         ConcurrentHashMap<String, SimpleMessageListenerContainer> roomFileDeleteListenersMap,
                                         @Qualifier("roomFileDeleteAdapter")
                                         MessageListenerAdapter roomFileDeleteAdapter) {
        super(connectionFactory);
        this.roomFileDeleteListenersMap = roomFileDeleteListenersMap;
        this.roomFileDeleteAdapter = roomFileDeleteAdapter;
    }

    public void startQueueListener(String queueName){
        startListeners(queueName, roomFileDeleteAdapter, roomFileDeleteListenersMap);
    }

    public void stopQueueListener(String queueName){
        stopListeners(queueName, roomFileDeleteListenersMap);
    }

}
