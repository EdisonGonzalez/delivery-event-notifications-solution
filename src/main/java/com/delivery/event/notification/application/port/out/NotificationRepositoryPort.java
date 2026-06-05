package com.delivery.event.notification.application.port.out;

import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for notification events.
 */
public interface NotificationRepositoryPort {

    List<NotificationEvent> findAllByClientIdAndFilter(String clientId, DeliveryStatus status, Instant from, Instant to);

    Optional<NotificationEvent> findByIdAndClientId(UUID notificationEventId, String clientId);

    Optional<NotificationEvent> findById(UUID notificationEventId);

    List<NotificationEvent> findClaimableBatch(Instant now, int batchSize);

    NotificationEvent save(NotificationEvent notificationEvent);

    long countPending();
}

