package com.delivery.event.notification.infrastructure.web;

import com.delivery.event.notification.domain.exception.NotificationNotFoundException;
import com.delivery.event.notification.domain.exception.NotificationReplayException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Converts application exceptions into safe API responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP = "timestamp";
    private static final String ERROR = "error";

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotificationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(TIMESTAMP, Instant.now().toString(), ERROR, ex.getMessage()));
    }

    @ExceptionHandler(NotificationReplayException.class)
    public ResponseEntity<Map<String, Object>> handleReplay(NotificationReplayException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(TIMESTAMP, Instant.now().toString(), ERROR, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(TIMESTAMP, Instant.now().toString(), ERROR, ex.getMessage()));
    }
}

