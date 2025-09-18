package com.victor.EventDrop.exceptionhandler;

import com.victor.EventDrop.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            NoSuchRoomException.class,
            NoSuchFileDropException.class
    })
    public ResponseEntity<ApiError> handleNotFoundExceptions(Exception e) {
        ApiError apiError = new ApiError(404, e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "You don't have permission to perform this action because you aren't the room owner");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler({
            RoomTtlExceededException.class,
            FileDropThresholdExceededException.class
    })
    public ResponseEntity<ApiError> handleConflictExceptions(Exception e) {
        ApiError apiError = new ApiError(409, e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({
            RoomCreationException.class,
            RoomDeletionException.class,
            OccupantCreationException.class,
            OccupantDeletionException.class,
            FileDropUploadException.class,
            FileDropDownloadException.class,
            AzureException.class
    })
    public ResponseEntity<ApiError> handleInternalServerExceptions(Exception e) {
        ApiError apiError = new ApiError(500, e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception e) {
        ApiError apiError = new ApiError(500, e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}