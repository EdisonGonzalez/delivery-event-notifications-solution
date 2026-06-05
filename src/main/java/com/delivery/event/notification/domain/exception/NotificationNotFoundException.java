package com.delivery.event.notification.domain.exception;

/**
 * Raised when a client tries to access a notification that does not exist or does not belong to them.
 */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(String message) {
        super(message);
    }
}

