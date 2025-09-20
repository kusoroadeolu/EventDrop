package com.victor.EventDrop.occupants;

import com.victor.EventDrop.exceptions.OccupantDeletionException;
import com.victor.EventDrop.rooms.events.RoomExpiryEvent;
import com.victor.EventDrop.rooms.events.RoomJoinEvent;
import com.victor.EventDrop.rooms.events.RoomLeaveEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OccupantServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private OccupantRepository occupantRepository;

    @InjectMocks
    private OccupantServiceImpl occupantService;

    private RoomJoinEvent roomJoinEvent;


    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        Field field = OccupantServiceImpl.class.getDeclaredField("maxRoomSize");
        field.setAccessible(true);
        field.setInt(occupantService, 2);
        roomJoinEvent = new RoomJoinEvent(
                "username",
                UUID.randomUUID(),
                OccupantRole.OCCUPANT,
                "12345678",
                LocalDateTime.now().plusMinutes(10)
        );
    }

    @Test
    void createOccupant_shouldReturn200_whenRoomNotFull() throws IllegalAccessException, NoSuchFieldException {
        //Arrange
        when(occupantRepository.findByRoomCode(roomJoinEvent.roomCode())).thenReturn(List.of());

        OccupantRoomJoinResponse response = occupantService.createOccupant(roomJoinEvent);

        assertNotNull(response);
        assertEquals(200, response.status());
        assertTrue(response.success());
        verify(occupantRepository, times(1)).save(any(Occupant.class));
    }

    @Test
    void createOccupant_shouldReturn409_whenRoomFull() throws IllegalAccessException, NoSuchFieldException {
        when(occupantRepository.findByRoomCode(roomJoinEvent.roomCode()))
                .thenReturn(List.of(new Occupant(), new Occupant()));

        OccupantRoomJoinResponse response = occupantService.createOccupant(roomJoinEvent);

        assertEquals(409, response.status());
        assertFalse(response.success());
        verify(occupantRepository, never()).save(any());
    }

    @Test
    void createOccupant_shouldReturn500_whenSaveThrowsException() {
        when(occupantRepository.findByRoomCode(roomJoinEvent.roomCode())).thenReturn(List.of());
        when(occupantRepository.save(any())).thenThrow(new RuntimeException());

        OccupantRoomJoinResponse response = occupantService.createOccupant(roomJoinEvent);

        assertEquals(500, response.status());
        assertFalse(response.success());
    }

    @Test
    void deleteOccupant_shouldCallDelete_whenOccupantExists() {
        RoomLeaveEvent leaveEvent = new RoomLeaveEvent(
                roomJoinEvent.roomCode(),
                roomJoinEvent.username(),
                roomJoinEvent.sessionId()
        );
        Occupant occupant = Occupant.builder()
                .roomCode(leaveEvent.roomCode())
                .occupantName(leaveEvent.occupantName())
                .sessionId(leaveEvent.sessionId())
                .build();

        when(occupantRepository.findBySessionId(leaveEvent.sessionId().toString())).thenReturn(occupant);

        assertDoesNotThrow(() -> occupantService.deleteOccupant(leaveEvent));
        verify(occupantRepository, times(1))
                .deleteByRoomCodeAndSessionId(occupant.getRoomCode(), occupant.getSessionId().toString());
    }

    @Test
    void deleteOccupant_shouldThrowOccupantDeletionException_whenDeleteFails() {
        RoomLeaveEvent leaveEvent = new RoomLeaveEvent(
                roomJoinEvent.roomCode(),
                roomJoinEvent.username(),
                roomJoinEvent.sessionId()
        );
        Occupant occupant = Occupant.builder()
                .roomCode(leaveEvent.roomCode())
                .occupantName(leaveEvent.occupantName())
                .sessionId(leaveEvent.sessionId())
                .build();

        when(occupantRepository.findBySessionId(leaveEvent.sessionId().toString())).thenReturn(occupant);
        doThrow(new RuntimeException()).when(occupantRepository)
                .deleteByRoomCodeAndSessionId(occupant.getRoomCode(), occupant.getSessionId().toString());

        assertThrows(OccupantDeletionException.class, () -> occupantService.deleteOccupant(leaveEvent));
    }


    @Test
    void getOccupantCount_shouldReturnCorrectSize() {
        when(occupantRepository.findByRoomCode("room1")).thenReturn(List.of(new Occupant(), new Occupant()));
        int count = occupantService.getOccupantCount("room1");
        assertEquals(2, count);
    }


    @Test
    void handleRoomExpiry_shouldExpireAllOccupants() {
        RoomExpiryEvent expiryEvent = new RoomExpiryEvent("room1");
        Occupant o1 = mock(Occupant.class);
        Occupant o2 = mock(Occupant.class);
        when(o1.getSessionId()).thenReturn(UUID.randomUUID());
        when(o2.getSessionId()).thenReturn(UUID.randomUUID());
        when(occupantRepository.findByRoomCode("room1")).thenReturn(List.of(o1, o2));

        assertDoesNotThrow(() -> occupantService.handleRoomExpiry(expiryEvent));
        verify(redisTemplate, times(2)).expire(anyString(), eq(Duration.ofSeconds(2)));
    }

    @Test
    void handleRoomExpiry_shouldReturnIfNoOccupants() {
        RoomExpiryEvent expiryEvent = new RoomExpiryEvent("room1");
        when(occupantRepository.findByRoomCode("room1")).thenReturn(List.of());

        assertDoesNotThrow(() -> occupantService.handleRoomExpiry(expiryEvent));
        verify(redisTemplate, never()).expire(anyString(), any());
    }

    @Test
    void handleSessionExpiry_shouldDeleteValidUUIDOccupant() {
        UUID uuid = UUID.randomUUID();
        @SuppressWarnings("unchecked")
        RedisKeyExpiredEvent<Occupant> expiredEvent = mock(RedisKeyExpiredEvent.class);
        when(expiredEvent.getId()).thenReturn(uuid.toString().getBytes());

        assertDoesNotThrow(() -> occupantService.handleSessionExpiry(expiredEvent));
        verify(occupantRepository, times(1)).deleteBySessionId(uuid.toString());
    }

    @Test
    void handleSessionExpiry_shouldReturnIfEmptyKey() {
        @SuppressWarnings("unchecked")
        RedisKeyExpiredEvent<Occupant> expiredEvent = mock(RedisKeyExpiredEvent.class);
        when(expiredEvent.getId()).thenReturn(new byte[0]);

        assertDoesNotThrow(() -> occupantService.handleSessionExpiry(expiredEvent));
        verify(occupantRepository, never()).deleteBySessionId(any());
    }

    @Test
    void handleSessionExpiry_shouldReturnIfInvalidUUID() {
        @SuppressWarnings("unchecked")
        RedisKeyExpiredEvent<Occupant> expiredEvent = mock(RedisKeyExpiredEvent.class);
        when(expiredEvent.getId()).thenReturn("invalid-uuid".getBytes());

        assertDoesNotThrow(() -> occupantService.handleSessionExpiry(expiredEvent));
        verify(occupantRepository, never()).deleteBySessionId(any());
    }

    @Test
    void handleSessionExpiry_shouldReturnIfShortSessionId() {
        @SuppressWarnings("unchecked")
        RedisKeyExpiredEvent<Occupant> expiredEvent = mock(RedisKeyExpiredEvent.class);
        when(expiredEvent.getId()).thenReturn("12345678".getBytes()); // <=8 characters

        assertDoesNotThrow(() -> occupantService.handleSessionExpiry(expiredEvent));
        verify(occupantRepository, never()).deleteBySessionId(any());
    }
}
