package com.delivery.event.notification.application.port.in;

import com.delivery.event.notification.domain.model.NotificationEvent;

/**
 * Use case for creating notification events (simulation/testing only).
 *
 * <p>This use case is intended for development and testing purposes only to simulate the arrival
 * of a new notification event.
 */
public interface CreateNotificationEventUseCase {

  /**
   * Creates a new notification event.
   *
   * @param clientId The client ID that owns this event.
   * @param eventId Business identifier for the event.
   * @param eventType Type classification of the event.
   * @param content The event payload/message.
   * @param webhookUrl The target URL for webhook delivery.
   * @return The created NotificationEvent.
   */
  NotificationEvent create(
      String clientId, String eventId, String eventType, String content, String webhookUrl);
}

