package com.victor.EventDrop.exceptions;

public class OccupantDeletionException extends RuntimeException {
    public OccupantDeletionException(String message, Throwable cause) {
        super(message, cause);
    }

    public OccupantDeletionException(String message) {
        super(message);
    }
}
