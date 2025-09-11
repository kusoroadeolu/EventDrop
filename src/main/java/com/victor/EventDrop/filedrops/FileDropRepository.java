package com.victor.EventDrop.filedrops;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface FileDropRepository extends CrudRepository<FileDrop, UUID> {

}
