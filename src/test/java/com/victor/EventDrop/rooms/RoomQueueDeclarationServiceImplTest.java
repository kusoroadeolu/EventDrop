package com.victor.EventDrop.rooms;

import com.victor.EventDrop.rooms.configproperties.RoomJoinConfigProperties;
import com.victor.EventDrop.rooms.configproperties.RoomLeaveConfigProperties;
import com.victor.EventDrop.rooms.dtos.RoomQueueDeclareDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomQueueDeclarationServiceImplTest {

    @Mock private RabbitAdmin rabbitAdmin;
    @Mock private DirectExchange joinExchange;
    @Mock private DirectExchange leaveExchange;
    @Mock private RoomJoinConfigProperties joinProps;
    @Mock private RoomLeaveConfigProperties leaveProps;

    @InjectMocks
    private RoomQueueDeclarationServiceImpl service;

    @Test
    void declareRoomQueueAndBinding_shouldDeclareQueueAndBinding() {
        // Arrange
        RoomQueueDeclareDto dto = new RoomQueueDeclareDto("prefix.q.", "prefix.rk.", "room123");
        when(joinExchange.getName()).thenReturn("join-ex");

        // Act
        String result = service.declareRoomQueueAndBinding(dto, joinExchange);

        // Assert
        assertNotNull(result);
        assertEquals("prefix.q.room123", result);

        verify(rabbitAdmin).declareQueue(argThat(queue ->
                "prefix.q.room123".equals(queue.getName()) && queue.isDurable()
        ));
        verify(rabbitAdmin).declareBinding(argThat(binding ->
                "prefix.rk.room123".equals(binding.getRoutingKey()) &&
                        "join-ex".equals(binding.getExchange())
        ));
    }

    @Test
    void declareRoomJoinQueueAndBinding_shouldUseJoinProps() {
        // Arrange
        when(joinProps.getQueuePrefix()).thenReturn("join.q.");
        when(joinProps.getRoutingKeyPrefix()).thenReturn("join.rk.");

        // Act
        String result = service.declareRoomJoinQueueAndBinding("abc");

        //Assert
        assertNotNull(result);
        assertEquals("join.q.abc", result);

        verify(rabbitAdmin).declareQueue(any(Queue.class));
        verify(rabbitAdmin).declareBinding(any(Binding.class));
    }

    @Test
    void declareRoomLeaveQueueAndBinding_shouldUseLeaveProps() {
        //Arrange
        when(leaveProps.getQueuePrefix()).thenReturn("leave.q.");
        when(leaveProps.getRoutingKeyPrefix()).thenReturn("leave.rk.");

        // Act
        String result = service.declareRoomLeaveQueueAndBinding("xyz");

        // Assert
        assertNotNull(result);
        assertEquals("leave.q.xyz", result);

        verify(rabbitAdmin).declareQueue(any(Queue.class));
        verify(rabbitAdmin).declareBinding(any(Binding.class));
    }

    @Test
    void deleteAllQueues_shouldDeleteBothQueues() {
        //Arrange
        when(joinProps.getQueuePrefix()).thenReturn("join.q.");
        when(leaveProps.getQueuePrefix()).thenReturn("leave.q.");

        // when
        service.deleteAllQueues("room42");

        // then
        verify(rabbitAdmin).deleteQueue("join.q.room42");
        verify(rabbitAdmin).deleteQueue("leave.q.room42");
    }


}
