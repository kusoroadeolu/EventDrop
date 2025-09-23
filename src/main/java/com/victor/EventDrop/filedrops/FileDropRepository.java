package com.victor.EventDrop.filedrops;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface FileDropRepository extends CrudRepository<FileDrop, UUID> {

    List<FileDrop> findByRoomCode(String roomCode);

    void deleteByRoomCode(String roomCode);

    int countByRoomCode(String roomCode);

    boolean existsByFileName(String fileDropName);

    FileDrop findByFileName(String fileDropName);
}
