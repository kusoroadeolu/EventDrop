package com.victor.EventDrop.occupants;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OccupantRepository extends CrudRepository<Occupant, UUID> {
    Occupant findBySessionId(String sessionId);

    void deleteByRoomCode(String roomCode);

    int countByRoomCode(String roomCode);

    void deleteByRoomCodeAndSessionId(String roomCode, String sessionId);

    void deleteBySessionId(String sessionId);

    List<Occupant> findByRoomCode(String roomCode);
}
