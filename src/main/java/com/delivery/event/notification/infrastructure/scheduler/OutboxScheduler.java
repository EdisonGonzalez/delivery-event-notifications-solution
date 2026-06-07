package com.delivery.event.notification.infrastructure.scheduler;

import com.delivery.event.notification.application.port.in.ProcessNotificationUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduled trigger that drives outbox processing. */
@Component
public class OutboxScheduler {

  private final ProcessNotificationUseCase processNotificationUseCase;

  /**
   * @param processNotificationUseCase Use case for processing notification events
   */
  public OutboxScheduler(ProcessNotificationUseCase processNotificationUseCase) {
    this.processNotificationUseCase = processNotificationUseCase;
  }

  /** Executes outbox processing periodically. */
  @Scheduled(fixedDelayString = "${notification.scheduler.fixed-delay-ms:5000}")
  public void processPendingNotifications() {
    processNotificationUseCase.processBatch();
  }
}
