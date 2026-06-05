package com.delivery.event.notification.domain.exception;

/**
 * Raised when replay cannot be executed.
 */
public class NotificationReplayException extends RuntimeException {

    public NotificationReplayException(String message) {
        super(message);
    }
}

