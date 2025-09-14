package com.victor.EventDrop.exceptions;

public class RoomTtlExceededException extends RuntimeException {
    public RoomTtlExceededException(String message) {
        super(message);
    }
}
