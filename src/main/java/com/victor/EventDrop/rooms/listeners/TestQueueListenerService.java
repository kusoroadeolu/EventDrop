package com.victor.EventDrop.rooms.listeners;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

//A class just for testing abstract queue listener
public class TestQueueListenerService extends AbstractQueueListenerService{

    public TestQueueListenerService(CachingConnectionFactory connectionFactory) {
        super(connectionFactory);
    }
}
