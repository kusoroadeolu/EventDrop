package com.victor.EventDrop.exceptions;

public class OccupantCreationException extends RuntimeException {
    public OccupantCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public OccupantCreationException(String message) {
        super(message);
    }
}
