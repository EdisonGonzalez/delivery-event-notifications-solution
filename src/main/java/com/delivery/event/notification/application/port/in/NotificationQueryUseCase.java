package com.delivery.event.notification.application.port.in;

import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use case for querying notification events by the authenticated client.
 */
public interface NotificationQueryUseCase {

    List<NotificationEvent> findAll(String clientId, DeliveryStatus status, Instant from, Instant to);

    NotificationEvent findById(String clientId, UUID notificationEventId);
}

