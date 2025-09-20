package com.victor.EventDrop.rooms.listeners;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AbstractQueueListenerServiceTest {

    @Mock
    private CachingConnectionFactory connectionFactory;

    @Mock
    private MessageListenerAdapter adapter;

    private ConcurrentHashMap<String, SimpleMessageListenerContainer> listenerMap;

    private TestQueueListenerService service;

    @BeforeEach
    void setUp() {
        listenerMap = new ConcurrentHashMap<>();
        service = new TestQueueListenerService(connectionFactory);
    }

    @Test
    void startListeners_shouldAddListenerToMap() {
        String queueName = "roomQueue";

        service.startListeners(queueName, adapter, listenerMap);

        assertNotNull(listenerMap.get(queueName)); // listener container should exist
        assertFalse(listenerMap.isEmpty());
    }

    @Test
    void startListeners_shouldDoNothingIfQueueAlreadyExists() {
        String queueName = "roomQueue";
        SimpleMessageListenerContainer container = mock(SimpleMessageListenerContainer.class);
        listenerMap.put(queueName, container);

        service.startListeners(queueName, adapter, listenerMap);

        assertEquals(container, listenerMap.get(queueName)); // original container stays
    }

    @Test
    void startListeners_shouldThrowForNullQueueName() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.startListeners(null, adapter, listenerMap));

        assertEquals("Queue name cannot be null or empty", ex.getMessage());
    }

    @Test
    void stopListeners_shouldRemoveListenerFromMap() {
        String queueName = "roomQueue";
        SimpleMessageListenerContainer container = mock(SimpleMessageListenerContainer.class);
        listenerMap.put(queueName, container);

        service.stopListeners(queueName, listenerMap);

        assertFalse(listenerMap.containsKey(queueName));
        verify(container, times(1)).stop();
    }

    @Test
    void stopListeners_shouldDoNothingIfListenerNotFound() {
        String queueName = "nonexistentQueue";

        service.stopListeners(queueName, listenerMap);

        assertTrue(listenerMap.isEmpty());
    }
}
