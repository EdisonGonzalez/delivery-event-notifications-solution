package com.delivery.event.notification.domain.exception;

/** Raised when replay cannot be executed. */
public class NotificationReplayException extends RuntimeException {

  /**
   * Constructs a new NotificationReplayException with the specified message.
   *
   * @param message the message to be set
   */
  public NotificationReplayException(String message) {
    super(message);
  }
}
