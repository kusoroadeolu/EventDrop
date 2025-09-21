package com.victor.EventDrop.exceptionhandler;

import com.victor.EventDrop.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler({
            RateLimitExceededException.class
    })
    public ResponseEntity<ApiError> handleRateLimitExceededExceptions(Exception e) {
        ApiError apiError = new ApiError(429, e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(apiError, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        ApiError apiError = new ApiError(403, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiError);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiError> handleAuthDenied(AuthorizationDeniedException ex) {
        ApiError apiError = new ApiError(401, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiError);
    }

    @ExceptionHandler({
            RoomTtlExceededException.class,
            RoomFullException.class,
            FileDropThresholdExceededException.class
    })
    public ResponseEntity<ApiError> handleConflictExceptions(Exception e) {
        ApiError apiError = new ApiError(409, e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({
            RoomCreationException.class,
            RoomDeletionException.class,
            RoomJoinException.class,
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception e) {
        ApiError apiError = new ApiError(500, e.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}