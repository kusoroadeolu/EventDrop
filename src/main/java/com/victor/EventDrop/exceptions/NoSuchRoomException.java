package com.victor.EventDrop.exceptions;

public class NoSuchRoomException extends RuntimeException {

    public NoSuchRoomException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchRoomException(String message) {
        super(message);
    }
}
