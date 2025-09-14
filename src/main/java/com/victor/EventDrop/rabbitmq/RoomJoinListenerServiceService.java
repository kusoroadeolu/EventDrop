package com.victor.EventDrop.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RoomJoinListenerServiceService extends AbstractQueueListenerService {

    private final MessageListenerAdapter roomJoinAdapter;
    private final ConcurrentHashMap<String, SimpleMessageListenerContainer> roomJoinListenersMap;

    public RoomJoinListenerServiceService(CachingConnectionFactory connectionFactory,
                                          @Qualifier("roomJoinAdapter")
                                          MessageListenerAdapter roomJoinAdapter,
                                          @Qualifier("roomJoinListenersMap")
                                          ConcurrentHashMap<String, SimpleMessageListenerContainer> roomJoinListenersMap) {
        super(connectionFactory);
        this.roomJoinAdapter = roomJoinAdapter;
        this.roomJoinListenersMap = roomJoinListenersMap;
    }

    /**
     * Starts a new listener for the specified queue.
     * @param queueName The name of the queue to listen to.
     */
    public void startQueueListener(String queueName){
        startListeners(queueName, roomJoinAdapter, roomJoinListenersMap);
    }

    /**
     * Stops and removes the listener for the specified queue.
     * @param queueName The name of the queue.
     */
    public void stopListener(String queueName){
        var listener = roomJoinListenersMap.get(queueName);

        if(listener != null){
            listener.stop();
            roomJoinListenersMap.remove(queueName);
            log.info("Successfully stopped queue listener for queue: {}", queueName);
        }else{
            log.info("Found no active listener for queue: {}", queueName);
        }
    }



}
