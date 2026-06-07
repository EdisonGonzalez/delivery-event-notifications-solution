package com.delivery.event.notification.application.service;

import com.delivery.event.notification.application.port.in.CreateNotificationEventUseCase;
import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of the create notification event use case.
 *
 * <p>This service is for development/testing purposes only to simulate the arrival of new
 * notification events.
 */
public class CreateNotificationEventService implements CreateNotificationEventUseCase {

  private final NotificationRepositoryPort notificationRepositoryPort;

  /**
   * @param notificationRepositoryPort Repository port for persistence.
   */
  public CreateNotificationEventService(NotificationRepositoryPort notificationRepositoryPort) {
    this.notificationRepositoryPort = notificationRepositoryPort;
  }

  /** {@inheritDoc} */
  @Override
  public NotificationEvent create(
      String clientId, String eventId, String eventType, String content, String webhookUrl) {
    Instant now = Instant.now();
    NotificationEvent event =
        NotificationEvent.builder()
            .eventId(eventId)
            .eventType(eventType)
            .clientId(clientId)
            .content(content)
            .webhookUrl(webhookUrl)
            .deliveryStatus(DeliveryStatus.PENDING)
            .retryCount(0)
            .reason(null)
            .nextAttemptAt(now)
            .createdAt(now)
            .updatedAt(now)
            .channel("api")
            .correlationId(UUID.randomUUID().toString())
            .build();

    return notificationRepositoryPort.save(event);
  }
}

