package com.victor.EventDrop.exceptions;

public class RoomJoinException extends RuntimeException {

    public RoomJoinException(String message, Throwable cause) {
        super(message, cause);
    }

    public RoomJoinException(String message) {
        super(message);
    }
}
