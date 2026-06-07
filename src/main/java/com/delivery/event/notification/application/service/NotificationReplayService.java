package com.delivery.event.notification.application.service;

import com.delivery.event.notification.application.port.in.NotificationReplayUseCase;
import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.domain.exception.NotificationNotFoundException;
import com.delivery.event.notification.domain.exception.NotificationReplayException;
import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import java.time.Instant;
import java.util.UUID;

/** Replay use case implementation. */
public class NotificationReplayService implements NotificationReplayUseCase {

  private final NotificationRepositoryPort notificationRepositoryPort;

  /**
   * @param notificationRepositoryPort Persistence port used to query and reset events.
   */
  public NotificationReplayService(NotificationRepositoryPort notificationRepositoryPort) {
    this.notificationRepositoryPort = notificationRepositoryPort;
  }

  /** {@inheritDoc} */
  @Override
  public void replay(String clientId, UUID notificationEventId) {
    NotificationEvent notificationEvent =
        notificationRepositoryPort
            .findByIdAndClientId(notificationEventId, clientId)
            .orElseThrow(() -> new NotificationNotFoundException("Notification event not found"));

    if (notificationEvent.getDeliveryStatus() != DeliveryStatus.FAILED) {
      throw new NotificationReplayException("Only failed notifications can be replayed");
    }

    notificationEvent.markPending();
    notificationEvent.setNextAttemptAt(Instant.now());
    notificationRepositoryPort.save(notificationEvent);
  }
}
