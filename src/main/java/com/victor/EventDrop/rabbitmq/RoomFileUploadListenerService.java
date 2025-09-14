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
public class RoomFileUploadListenerService extends AbstractQueueListenerService {

    private final MessageListenerAdapter roomFileUploadAdapter;

    private final ConcurrentHashMap<String, SimpleMessageListenerContainer> roomFileUploadListenersMap;

    public RoomFileUploadListenerService(CachingConnectionFactory connectionFactory,
                                         @Qualifier("roomFileUploadAdapter")
                                         MessageListenerAdapter roomFileUploadAdapter,
                                         @Qualifier("roomFileUploadListenersMap")
                                         ConcurrentHashMap<String, SimpleMessageListenerContainer> fileUploadListenersMap) {
        super(connectionFactory);
        this.roomFileUploadAdapter = roomFileUploadAdapter;
        this.roomFileUploadListenersMap = fileUploadListenersMap;
    }

    public void startQueueListener(String queueName){
        startListeners(queueName, roomFileUploadAdapter, roomFileUploadListenersMap);
    }

    public void stopQueueListener(String queueName){
        stopListeners(queueName, roomFileUploadListenersMap);
    }

}
