package com.victor.EventDrop.exceptions;

public class AzureException extends RuntimeException {
    public AzureException(String message, Throwable cause) {
        super(message, cause);
    }

    public AzureException(String message) {
        super(message);
    }
}
