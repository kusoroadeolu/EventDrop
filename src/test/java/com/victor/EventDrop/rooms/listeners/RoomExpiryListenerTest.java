package com.victor.EventDrop.rooms.listeners;

import com.victor.EventDrop.rooms.Room;
import com.victor.EventDrop.rooms.RoomEmitterHandler;
import com.victor.EventDrop.rooms.RoomService;
import com.victor.EventDrop.rooms.configproperties.RoomExpiryConfigProperties;
import com.victor.EventDrop.rooms.events.RoomEvent;
import com.victor.EventDrop.rooms.events.RoomExpiryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomExpiryListenerTest {
    @Mock
    private RoomService roomService;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private RoomExpiryConfigProperties roomExpiryConfigProperties;
    @Mock
    private RoomEmitterHandler roomEmitterHandler;
    @Mock
    private  ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private RoomExpiryListener roomExpiryListener;

    @Test
    public void handleRoomExpiry_shouldCompleteExpiryChain(){
        //Arrange
        @SuppressWarnings("unchecked")
        RedisKeyExpiredEvent<Room> expiredEvent = Mockito.mock(RedisKeyExpiredEvent.class);
        when(expiredEvent.getId()).thenReturn(new byte[]{ '1', '2', '3', '4', '5', '6', '7', '8'});
        when(roomExpiryConfigProperties.getExchangeName()).thenReturn("rm.ex");
        when(roomExpiryConfigProperties.getRoutingKey()).thenReturn("rm.rk");

        //Act
        roomExpiryListener.handleRoomExpiry(expiredEvent);

        //Assert
        verify(roomService, times(1)).deleteByRoomCode(anyString());
        verify(applicationEventPublisher, times(1)).publishEvent(any(RoomEvent.class));
        verify(roomEmitterHandler, times(1)).removeRoomEmitters(anyString());
        verify(rabbitTemplate, times(1)).convertAndSend(
                anyString(), anyString(), any(RoomExpiryEvent.class)
        );

    }

    @Test
    public void handleRoomExpiry_shouldDoNothingGivenInvalidRoomCode(){
        //Arrange
        @SuppressWarnings("unchecked")
        RedisKeyExpiredEvent<Room> expiredEvent = Mockito.mock(RedisKeyExpiredEvent.class);
        when(expiredEvent.getId()).thenReturn(new byte[]{ '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' });

        //Act
        roomExpiryListener.handleRoomExpiry(expiredEvent);

        //Assert
        verify(applicationEventPublisher, times(0)).publishEvent(any(RoomEvent.class));
        verify(roomService, times(0)).deleteByRoomCode(anyString());

    }




}