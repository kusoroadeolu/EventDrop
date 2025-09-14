package com.victor.EventDrop.exceptions;

public class FileDropThresholdExceededException extends RuntimeException {
    public FileDropThresholdExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileDropThresholdExceededException(String message) {
        super(message);
    }
}
