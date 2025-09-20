package com.victor.EventDrop.rooms;

import com.victor.EventDrop.exceptions.*;
import com.victor.EventDrop.occupants.Occupant;
import com.victor.EventDrop.occupants.OccupantRole;
import com.victor.EventDrop.occupants.OccupantRoomJoinResponse;
import com.victor.EventDrop.rooms.configproperties.RoomJoinConfigProperties;
import com.victor.EventDrop.rooms.configproperties.RoomLeaveConfigProperties;
import com.victor.EventDrop.rooms.dtos.RoomCreateRequestDto;
import com.victor.EventDrop.rooms.dtos.RoomJoinRequestDto;
import com.victor.EventDrop.rooms.dtos.RoomJoinResponseDto;
import com.victor.EventDrop.rooms.events.RoomEvent;
import com.victor.EventDrop.rooms.events.RoomJoinEvent;
import com.victor.EventDrop.rooms.events.RoomLeaveEvent;
import com.victor.EventDrop.rooms.listeners.RoomQueueListenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomJoinConfigProperties roomJoinConfigProperties;
    @Mock
    private RoomLeaveConfigProperties roomLeaveConfigProperties;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private SecureRandom secureRandom;
    @Mock
    private RoomQueueListenerService roomQueueListenerService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    @Spy
    private RoomServiceImpl roomService;

    private Room room;
    private RoomCreateRequestDto roomCreateRequestDto;
    private RoomJoinRequestDto roomJoinRequestDto;
    private RoomJoinResponseDto roomJoinResponseDto;
    private UUID sessionIdAsUuid;
    private String sessionId;

    @BeforeEach
    public void setUp(){
        sessionIdAsUuid = UUID.randomUUID();
        sessionId = sessionIdAsUuid.toString();
        room = Room
                .builder()
                .roomCode("1234ABCD")
                .roomName("room_name")
                .ttl(60)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        roomCreateRequestDto = new RoomCreateRequestDto(
                "room_name",
                1,
                "my_name"
        );

        roomJoinRequestDto = new RoomJoinRequestDto(
                "my_name",
                OccupantRole.OWNER,
                "1234ABCD"
        );

        roomJoinResponseDto = new RoomJoinResponseDto(
                "room_name",
                "my_name",
                "sesh_id",
                LocalDateTime.now().plusMinutes(5)
        );
    }


    @Test
    public void createRoom_shouldReturnRoomJoinResponseDto(){
        //Arrange
        double ttl = roomCreateRequestDto.ttl();
        OccupantRoomJoinResponse occupantRoomJoinResponse = new OccupantRoomJoinResponse(true, 200);

        doReturn("1234ABCD").when(roomService).validateTtlAndGenerateRoomCode(ttl);
        when(roomRepository.findByRoomCode(anyString())).thenReturn(Optional.of(room));
        try(MockedStatic<UUID> staticUuid = mockStatic(UUID.class)){
            staticUuid.when(UUID::randomUUID).thenReturn(sessionIdAsUuid);
            assertEquals(sessionIdAsUuid.toString(), sessionId);
            when(roomJoinConfigProperties.getRoutingKeyPrefix()).thenReturn("room-join-routing-key-");
            when(rabbitTemplate.convertSendAndReceive(
                    anyString(),
                    anyString(),
                    any(RoomJoinEvent.class)
            )).thenReturn(occupantRoomJoinResponse);
            when(roomJoinConfigProperties.getExchangeName()).thenReturn("room-join-exchange");
            when(roomMapper.toRoomJoinResponseDto(room, sessionId , "my_name")).thenReturn(roomJoinResponseDto);


            //Act
            RoomJoinResponseDto expectedResponseDto = roomService.createRoom(roomCreateRequestDto);

            //Assert
            assertNotNull(expectedResponseDto);
            assertEquals(roomJoinResponseDto, expectedResponseDto);
            verify(roomRepository, times(1)).save(any(Room.class));
            verify(roomQueueListenerService, times(1)).startListeners(anyString());
            verify(eventPublisher, times(2)).publishEvent(any(RoomEvent.class));
        }
    }


    @Test
    public void createRoom_shouldThrowRoomTtlExceededException_givenTtlGreaterThan3Days(){

        //Arrange
        RoomCreateRequestDto invalidRoomCreateRequestDto = new RoomCreateRequestDto(
                "room_name",
                4321,
                "my_name"
        );

        //Act && Assert
        RoomTtlExceededException ex = assertThrows(RoomTtlExceededException.class, () -> {
           roomService.createRoom(invalidRoomCreateRequestDto);
        });
        assertEquals("Room TTL of 4321 minutes exceeded the maximum of 0 minutes.", ex.getMessage());
    }

    @Test
    public void ensureUniqueDigitRoomCode_shouldReturnUnique8DigitRoomCode(){
        //Arrange
        when(roomRepository.existsByRoomCode(anyString())).thenReturn(false);

        //Act
        String roomCode = roomService.ensureUniqueRoomCode();

        //Assert
        assertNotNull(roomCode);
        assertEquals(8, roomCode.length());
        verify(roomRepository, times(1)).existsByRoomCode(anyString());
    }

    @Test
    public void ensureUniqueDigitRoomCode_shouldThrowRoomCreationException_whenRoomExistsByRoomCodeAfter5Attempts(){
        //Arrange
        String roomCode = "room-code";
        when(roomService.generateRoomCode()).thenReturn(roomCode, roomCode, roomCode, roomCode, roomCode);
        when(roomRepository.existsByRoomCode(roomCode)).thenReturn(true, true, true, true, true);

        //Act && Assert
        RoomCreationException ex = assertThrows(RoomCreationException.class, () -> {
            roomService.ensureUniqueRoomCode();
        });
        verify(roomService, times(5)).generateRoomCode();
        verify(roomRepository, times(5)).existsByRoomCode(roomCode);
        assertEquals("Failed to generate unique room code after 5 attempts", ex.getMessage());
    }

    @Test
    public void joinRoom_shouldReturnRoomJoinResponseDto(){
        //Arrange
        OccupantRoomJoinResponse occupantRoomJoinResponse = new OccupantRoomJoinResponse(true, 200);

        when(roomRepository.findByRoomCode(anyString())).thenReturn(Optional.of(room));
        try(MockedStatic<UUID> staticUuid = mockStatic(UUID.class)){
            staticUuid.when(UUID::randomUUID).thenReturn(sessionIdAsUuid);
            assertEquals(sessionIdAsUuid.toString(), sessionId);
            when(roomJoinConfigProperties.getRoutingKeyPrefix()).thenReturn("room-join-routing-key-");
            when(rabbitTemplate.convertSendAndReceive(
                    anyString(),
                    anyString(),
                    any(RoomJoinEvent.class)
            )).thenReturn(occupantRoomJoinResponse);
            when(roomJoinConfigProperties.getExchangeName()).thenReturn("room-join-exchange");
            when(roomMapper.toRoomJoinResponseDto(room, sessionId , "my_name")).thenReturn(roomJoinResponseDto);


            //Act
            RoomJoinResponseDto expectedResponseDto = roomService.joinRoom(roomJoinRequestDto);

            //Assert
            assertNotNull(expectedResponseDto);
            assertEquals(roomJoinResponseDto, expectedResponseDto);
            verify(eventPublisher, times(1)).publishEvent(any(RoomEvent.class));
        }
    }

    @Test
    public void handleRoomJoinResponse_shouldThrowRoomJoinException_givenNullJoinResponse(){
        //Arrange
        OccupantRoomJoinResponse occupantRoomJoinResponse = null;

        //Act
        RoomJoinException ex = assertThrows(RoomJoinException.class, () -> {
            roomService.handleRoomJoinResponse(occupantRoomJoinResponse);
        });
        assertEquals("Failed to join room because occupant room join response is null", ex.getMessage());
    }

    @Test
    public void handleRoomJoinResponse_shouldDoNothing_onSuccess(){
        //Arrange
        OccupantRoomJoinResponse occupantRoomJoinResponse = new OccupantRoomJoinResponse(true, 200);

        //Act
        doNothing().when(roomService).handleRoomJoinResponse(occupantRoomJoinResponse);

        //Assert
        assertDoesNotThrow(() -> {
            roomService.handleRoomJoinResponse(occupantRoomJoinResponse);
        });

    }

    @Test
    public void handleRoomJoinResponse_shouldThrowRoomFullException_given409StatusCode(){
        //Arrange
        OccupantRoomJoinResponse occupantRoomJoinResponse = new OccupantRoomJoinResponse(false, 409);

        //Act && Assert
        RoomFullException ex = assertThrows(RoomFullException.class, () -> {
            roomService.handleRoomJoinResponse(occupantRoomJoinResponse);
        });
        assertEquals("Cannot join room because room is full", ex.getMessage());

    }

    @Test
    public void handleRoomJoinResponse_shouldThrowRoomJoinException_given500StatusCode(){
        //Arrange
        OccupantRoomJoinResponse occupantRoomJoinResponse = new OccupantRoomJoinResponse(false, 500);

        //Act && Assert
        RoomJoinException ex = assertThrows(RoomJoinException.class, () -> {
            roomService.handleRoomJoinResponse(occupantRoomJoinResponse);
        });
        assertEquals("An unexpected error occurred while trying to join this room", ex.getMessage());

    }

    @Test
    public void leaveRoom_shouldLeaveRoom(){
        //Arrange
        Occupant occupant = mock(Occupant.class);

        when(occupant.getRoomCode()).thenReturn("room-code");
        when(roomRepository.findByRoomCode("room-code")).thenReturn(Optional.of(room));
        when(occupant.getOccupantName()).thenReturn("occupant-name");
        when(occupant.getSessionId()).thenReturn(UUID.randomUUID());
        when(roomLeaveConfigProperties.getRoutingKeyPrefix()).thenReturn("room-leave-routing-key-");
        when(roomLeaveConfigProperties.getExchangeName()).thenReturn("room-leave-exchange");


        //Act
        roomService.leaveRoom(occupant);

        //Assert
        verify(occupant, times(1)).getRoomCode();
        verify(occupant, times(1)).getOccupantName();
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(RoomLeaveEvent.class));
        verify(eventPublisher, times(1)).publishEvent(any(RoomEvent.class));
    }

    @Test
    public void deleteRoom_shouldDeleteRoom(){
        //Arrange
        Occupant occupant = mock(Occupant.class);

        when(occupant.getRoomCode()).thenReturn("room-code");
        when(roomRepository.findByRoomCode("room-code")).thenReturn(Optional.of(room));
        when(occupant.getOccupantName()).thenReturn("occupant-name");
        when(occupant.getSessionId()).thenReturn(UUID.randomUUID());
        when(roomLeaveConfigProperties.getRoutingKeyPrefix()).thenReturn("room-leave-routing-key-");
        when(roomLeaveConfigProperties.getExchangeName()).thenReturn("room-leave-exchange");
        when(occupant.getRoomCode()).thenReturn("room-code");


        //Act
        roomService.deleteRoom(occupant);

        //Assert
        verify(occupant, times(2)).getRoomCode();
        verify(occupant, times(1)).getOccupantName();
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(RoomLeaveEvent.class));
        verify(eventPublisher, times(1)).publishEvent(any(RoomEvent.class));
    }

    @Test
    public void findByRoomCode_shouldReturnRoom_ifRoomExists(){
        //Arrange
        String roomCode = "room-code";
        when(roomRepository.findByRoomCode(roomCode)).thenReturn(Optional.of(room));

        //Act
        Room expectedRoom = roomService.findByRoomCode(roomCode);

        //Assert
        assertNotNull(expectedRoom);
        assertEquals(room, expectedRoom);
    }

    @Test
    public void findByRoomCode_shouldThrowNoSuchRoomEx_ifRoomDoesNotExist(){
        //Arrange
        String roomCode = "room-code";
        when(roomRepository.findByRoomCode(roomCode)).thenReturn(Optional.empty());

        //Act && Assert
        assertThrows(NoSuchRoomException.class, () -> {
            roomService.findByRoomCode(roomCode);
        });
    }

    @Test
    public void deleteByRoomCode_shouldSuccessfullyDeleteRoom_givenRoomCode(){
        String roomCode = "room-code";

        //Act
        roomService.deleteByRoomCode(roomCode);

        //Assert
        verify(roomRepository, times(1)).deleteById(roomCode);
    }

    @Test
    public void deleteByRoomCode_shouldThrowRoomDeletionException_ifRoomDeleteThrowsGenericException(){
        String roomCode = "room-code";

        //Act
        doThrow(RuntimeException.class).when(roomRepository).deleteById(roomCode);

        //Assert
        assertThrows(RoomDeletionException.class, () -> {
            roomService.deleteByRoomCode(roomCode);
        });

    }

    @Test
    public void generateRoomCode_shouldGenerate8DigitRoomCode(){
        String str = roomService.generateRoomCode();

        //Assert
        assertNotNull(str);
        assertEquals(8, str.length());
    }





}

