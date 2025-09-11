package com.victor.EventDrop.rabbitmq;

import com.victor.EventDrop.occupants.OccupantService;
import com.victor.EventDrop.occupants.OccupantServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class RoomJoinListenerService {

    private final CachingConnectionFactory connectionFactory;
    private final MessageListenerAdapter roomJoinAdapter;
    private final Map<String, SimpleMessageListenerContainer> roomJoinListenersMap;



    /**
     * Starts a new listener for the specified queue.
     * @param queueName The name of the queue to listen to.
     */
    public void startListeners(String queueName){
     try{
        if (queueName == null || queueName.trim().isEmpty()) {
            throw new IllegalArgumentException("Queue name cannot be null or empty");
        }

        if(roomJoinListenersMap.containsKey(queueName)){
            log.info("Listener for queue '{}' is already running.", queueName);
            return;
        }

        SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        listenerContainer.setQueueNames(queueName);
        listenerContainer.setConnectionFactory(connectionFactory);
        listenerContainer.setMessageListener(roomJoinAdapter);
        listenerContainer.start();
        roomJoinListenersMap.put(queueName, listenerContainer);

        log.info("Started queue listener for queue: {}", queueName);
     }catch (Exception e){
         log.info("Failed to start queue listener for queue: {}", queueName);
         throw new AmqpException(String.format("Failed to start queue listener for queue: %s", queueName));
     }

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
