package com.delivery.event.notification.domain.exception;

/**
 * Raised when a client tries to access a notification that does not exist or does not belong to
 * them.
 */
public class NotificationNotFoundException extends RuntimeException {

  /**
   * Constructs a new NotificationNotFoundException with the specified message.
   *
   * @param message the message to be set
   */
  public NotificationNotFoundException(String message) {
    super(message);
  }
}
