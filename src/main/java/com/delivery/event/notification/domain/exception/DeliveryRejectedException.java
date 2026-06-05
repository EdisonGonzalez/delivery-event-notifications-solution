package com.delivery.event.notification.domain.exception;

/**
 * Raised when a webhook delivery is rejected by business rules.
 */
public class DeliveryRejectedException extends RuntimeException {

    public DeliveryRejectedException(String message) {
        super(message);
    }
}

