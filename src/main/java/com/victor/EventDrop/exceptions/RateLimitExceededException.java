package com.victor.EventDrop.exceptions;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super();
    }

    public RateLimitExceededException(String message) {
        super(message);
    }
}
