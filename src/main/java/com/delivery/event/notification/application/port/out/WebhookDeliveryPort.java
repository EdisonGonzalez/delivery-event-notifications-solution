package com.delivery.event.notification.application.port.out;

import com.delivery.event.notification.domain.model.NotificationEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Outbound port for sending webhook notifications.
 */
public interface WebhookDeliveryPort {

    CompletableFuture<Void> deliver(NotificationEvent notificationEvent);
}

