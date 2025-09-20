package com.victor.EventDrop.rooms.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractQueueListenerService {
    private final CachingConnectionFactory connectionFactory;

    /**
     * Starts a queue listener for a room
     * @param queueName The name of the queue
     * @param adapter The message listener adapter of the queue
     * @param map A concurrent hashmap keeping track of all listeners of a queue
     * */
    protected void startListeners(String queueName, MessageListenerAdapter adapter, ConcurrentHashMap<String, SimpleMessageListenerContainer> map){
        log.info("Starting listener for queue: {}", queueName);
        if (queueName == null || queueName.trim().isEmpty()){
            log.info("Queue name cannot be null or empty");
            throw new IllegalArgumentException("Queue name cannot be null or empty");
        }

        if(map.containsKey(queueName)){
            log.info("Listener for queue '{}' is already running.", queueName);
            return;
        }

        try{

            SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
            listenerContainer.setConnectionFactory(connectionFactory);
            listenerContainer.setMessageListener(adapter);
            listenerContainer.setQueueNames(queueName);
            listenerContainer.start();
            map.put(queueName, listenerContainer);
            log.info("Started room listener for queue: {}", queueName);
        }catch (Exception e){
            log.info("Failed to start room listener for queue: {}", queueName, e);
            throw new AmqpException(String.format("Failed to start room listener for queue: %s", queueName), e);
        }
    }

    /**
     * Stops and removes the listener for the specified queue.
     * @param queueName The name of the queue.
     * @param map The hashmap keeping track of the queue
     */
    protected void stopListeners(String queueName, ConcurrentHashMap<String, SimpleMessageListenerContainer> map){
        var listener = map.get(queueName);

        if(listener != null){
            listener.stop();
            map.remove(queueName);
            log.info("Successfully stopped queue listener for queue: {}", queueName);
        }else{
            log.info("Found no active listener for queue: {}", queueName);
        }
    }

}
