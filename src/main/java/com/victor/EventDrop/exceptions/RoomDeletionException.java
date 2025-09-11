package com.victor.EventDrop.exceptions;

public class RoomDeletionException extends RuntimeException {

    public RoomDeletionException(String message, Throwable cause) {
        super(message, cause);
    }

    public RoomDeletionException(String message) {
        super(message);
    }
}
