package com.victor.EventDrop.exceptions;

public class NoSuchFileDropException extends RuntimeException {
    public NoSuchFileDropException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchFileDropException(String message) {
        super(message);
    }
}
