package com.victor.EventDrop.occupants;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OccupantRepository extends CrudRepository<Occupant, UUID> {
    Occupant findBySessionId(String sessionId);
}
