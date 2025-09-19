package com.victor.EventDrop.rooms.listeners;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RoomLeaveListenerService extends AbstractQueueListenerService{
    private final ConcurrentHashMap<String, SimpleMessageListenerContainer> roomLeaveListenersMap;
    private final MessageListenerAdapter roomLeaveAdapter;

    public RoomLeaveListenerService(CachingConnectionFactory connectionFactory,
                                    @Qualifier("roomLeaveListenersMap")
                                    ConcurrentHashMap<String, SimpleMessageListenerContainer> roomLeaveListenersMap,
                                    @Qualifier("roomLeaveAdapter")
                                    MessageListenerAdapter roomLeaveAdapter) {
        super(connectionFactory);
        this.roomLeaveListenersMap = roomLeaveListenersMap;
        this.roomLeaveAdapter = roomLeaveAdapter;
    }

    public void startQueueListener(String queueName){
        startListeners(queueName, roomLeaveAdapter, roomLeaveListenersMap);
    }

    public void stopQueueListener(String queueName){
        stopListeners(queueName, roomLeaveListenersMap);
    }
}
