package com.delivery.event.notification.application.service;

import com.delivery.event.notification.application.port.in.ProcessNotificationUseCase;
import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.domain.model.NotificationEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Outbox processor service. */
public class ProcessNotificationService implements ProcessNotificationUseCase {

  private final NotificationRepositoryPort notificationRepositoryPort;
  private final NotificationDeliveryProcessor notificationDeliveryProcessor;

  /**
   * @param notificationRepositoryPort Outbox persistence port.
   * @param notificationDeliveryProcessor Component that executes webhook delivery.
   */
  public ProcessNotificationService(
      NotificationRepositoryPort notificationRepositoryPort,
      NotificationDeliveryProcessor notificationDeliveryProcessor) {
    this.notificationRepositoryPort = notificationRepositoryPort;
    this.notificationDeliveryProcessor = notificationDeliveryProcessor;
  }

  /** Processes a batch of pending events and delegates each event to the delivery processor. */
  @Override
  @Transactional
  public void processBatch() {
    List<NotificationEvent> claimable =
        notificationRepositoryPort.findClaimableBatch(java.time.Instant.now(), 100);
    for (NotificationEvent notificationEvent : claimable) {
      process(notificationEvent);
    }
  }

  /**
   * Processes a single pending event when present.
   *
   * @param notificationId Identifier of the event to process.
   */
  @Override
  public void processNotification(UUID notificationId) {
    notificationRepositoryPort.findById(notificationId).ifPresent(this::process);
  }

  private void process(NotificationEvent notificationEvent) {
    notificationDeliveryProcessor.deliver(notificationEvent);
  }
}
