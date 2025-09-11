package com.victor.EventDrop.rooms;

import com.victor.EventDrop.rooms.dtos.RoomCreateRequestDto;
import com.victor.EventDrop.rooms.dtos.RoomJoinResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RoomMapperTest {

    @Autowired
    private RoomMapper roomMapper;

    private RoomJoinResponseDto roomJoinResponseDto;
    private RoomCreateRequestDto roomCreateRequestDto;
    private Room room;

    @BeforeEach
    public void setUp(){
        roomJoinResponseDto = new RoomJoinResponseDto(
                "ABC12345",
                "My Awesome Room",
                "USR",
                "SESH ID",
                LocalDateTime.now().plusMinutes(5)

        );

        roomCreateRequestDto = new RoomCreateRequestDto("My Awesome Room", 7200, "My Awesome Name");

        room = new Room(
                "ABC12345",
                "My Awesome Room",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2),
                7200
        );
    }

    @Test
    void toResponseDto_shouldMapToRoomJoinResponseDto_givenRoomEntity() {
        // Act
        RoomJoinResponseDto result = roomMapper.toRoomJoinResponseDto(room, "SESH_ID", "USR");

        // Assert
        assertNotNull(result);
        assertEquals(room.getRoomName(), result.roomName());
        assertEquals(room.getRoomCode(), result.roomCode());
        assertEquals(room.getExpiresAt(), result.expiresAt());
    }

    @Test
    void toRoomEntity_shouldMapToEntity_givenRequestDto() {
        // Act
        Room result = roomMapper.toRoomEntity(roomCreateRequestDto);

        // Assert
        assertNotNull(result);

        // Test mapped fields
        assertEquals(roomCreateRequestDto.roomName(), result.getRoomName());
        assertEquals(roomCreateRequestDto.ttl(), result.getTtl());

        // Test ignored fields (should be null since they're ignored in mapping)
        assertNull(result.getRoomCode());
        assertNull(result.getCreatedAt());
        assertNull(result.getExpiresAt());

    }

    @Test
    void toRoomEntity_shouldHandleNullValues_givenRequestDtoWithNulls() {
        // Arrange
        RoomCreateRequestDto nullRequestDto = new RoomCreateRequestDto(null, 0, null);

        // Act
        Room result = roomMapper.toRoomEntity(nullRequestDto);

        // Assert
        assertNotNull(result);
        assertNull(result.getRoomName());
        assertEquals(nullRequestDto.ttl(), result.getTtl());
    }

    @Test
    void toRoomEntity_shouldHandleEmptyRoomName_givenRequestDto() {
        // Arrange
        RoomCreateRequestDto emptyNameDto = new RoomCreateRequestDto("", 3600, "My Awesome Name");

        // Act
        Room result = roomMapper.toRoomEntity(emptyNameDto);

        // Assert
        assertNotNull(result);
        assertEquals("", result.getRoomName());
        assertEquals(3600, result.getTtl());
    }
}