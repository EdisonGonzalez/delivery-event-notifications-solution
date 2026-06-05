package com.delivery.event.notification.application.service;

import com.delivery.event.notification.application.port.in.ProcessNotificationUseCase;
import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.domain.model.NotificationEvent;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Outbox processor service.
 */
public class ProcessNotificationService implements ProcessNotificationUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final NotificationDeliveryProcessor notificationDeliveryProcessor;

    public ProcessNotificationService(NotificationRepositoryPort notificationRepositoryPort,
                                      NotificationDeliveryProcessor notificationDeliveryProcessor) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.notificationDeliveryProcessor = notificationDeliveryProcessor;
    }

    @Override
    @Transactional
    public void processBatch() {
        List<NotificationEvent> claimable = notificationRepositoryPort.findClaimableBatch(java.time.Instant.now(), 100);
        for (NotificationEvent notificationEvent : claimable) {
            process(notificationEvent);
        }
    }

    @Override
    public void processNotification(UUID notificationId) {
        notificationRepositoryPort.findById(notificationId).ifPresent(this::process);
    }

    private void process(NotificationEvent notificationEvent) {
        notificationDeliveryProcessor.deliver(notificationEvent);
    }
}

