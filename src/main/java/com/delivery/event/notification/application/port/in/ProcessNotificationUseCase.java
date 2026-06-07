package com.delivery.event.notification.application.port.in;

import java.util.UUID;

/** Use case for processing pending notifications. */
public interface ProcessNotificationUseCase {

  /** Processes a batch of pending events eligible for delivery. */
  void processBatch();

  /**
   * Processes a single event by its identifier.
   *
   * @param notificationId Identifier of the pending event.
   */
  void processNotification(UUID notificationId);
}
