package com.victor.EventDrop.exceptions;

public class RoomCreationException extends RuntimeException {
    public RoomCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RoomCreationException(String message) {
        super(message);
    }
}
