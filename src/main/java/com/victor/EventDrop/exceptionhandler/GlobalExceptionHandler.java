package com.victor.EventDrop.exceptionhandler;

import com.victor.EventDrop.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({NoSuchRoomException.class})
    public ResponseEntity<ApiError> handleNoSuchRoomException(Exception e){
        ApiError apiError = new ApiError(404, e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({RoomCreationException.class, RoomDeletionException.class, OccupantCreationException.class})
    public ResponseEntity<ApiError> handleInternalServerExceptions(Exception e){
        ApiError apiError = new ApiError(500, e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception e){
        ApiError apiError = new ApiError(500, e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
