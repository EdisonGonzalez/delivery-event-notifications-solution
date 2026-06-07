package com.delivery.event.notification.application.service;

import com.delivery.event.notification.application.port.in.NotificationQueryUseCase;
import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.domain.exception.NotificationNotFoundException;
import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import com.delivery.event.notification.infrastructure.web.dto.Page;
import java.time.Instant;
import java.util.UUID;

/** Query use case implementation. */
public class NotificationQueryService implements NotificationQueryUseCase {

  private final NotificationRepositoryPort notificationRepositoryPort;

  /**
   * @param notificationRepositoryPort Read port for querying events.
   */
  public NotificationQueryService(NotificationRepositoryPort notificationRepositoryPort) {
    this.notificationRepositoryPort = notificationRepositoryPort;
  }

  /** {@inheritDoc} */
  @Override
  public Page<NotificationEvent> findAll(
      String clientId, DeliveryStatus status, Instant from, Instant to, int page, int size) {
    return notificationRepositoryPort.findAllByClientIdAndFilter(
        clientId, status, from, to, page, size);
  }

  /** {@inheritDoc} */
  @Override
  public NotificationEvent findById(String clientId, UUID notificationEventId) {
    return notificationRepositoryPort
        .findByIdAndClientId(notificationEventId, clientId)
        .orElseThrow(() -> new NotificationNotFoundException("Notification event not found"));
  }
}
