package com.delivery.event.notification.application.service;

import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.application.port.out.SubscriptionPort;
import com.delivery.event.notification.application.port.out.WebhookDeliveryPort;
import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import com.delivery.event.notification.domain.model.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;


/**
 * Handles the concrete delivery of a notification with retry support.
 */
public class NotificationDeliveryProcessor {

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final SubscriptionPort subscriptionPort;
    private final WebhookDeliveryPort webhookDeliveryPort;
    private final int maxAttempts;

    public NotificationDeliveryProcessor(NotificationRepositoryPort notificationRepositoryPort,
                                         SubscriptionPort subscriptionPort,
                                         WebhookDeliveryPort webhookDeliveryPort,
                                         @Value("${notification.retry.max-attempts:3}") int maxAttempts) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.subscriptionPort = subscriptionPort;
        this.webhookDeliveryPort = webhookDeliveryPort;
        this.maxAttempts = maxAttempts;
    }

    @Retryable(
            retryFor = RuntimeException.class,
            maxAttemptsExpression = "${notification.retry.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${notification.retry.initial-delay-ms:2000}", multiplierExpression = "${notification.retry.multiplier:2.0}")
    )
    public NotificationEvent deliver(NotificationEvent notificationEvent) {
        Subscription subscription = subscriptionPort.findActiveSubscription(notificationEvent.getClientId(), notificationEvent.getEventType())
                .orElse(null);

        if (subscription == null) {
            notificationEvent.markIgnored("No active subscription found");
            return notificationRepositoryPort.save(notificationEvent);
        }

        notificationEvent.setDeliveryStatus(DeliveryStatus.IN_PROGRESS);
        notificationRepositoryPort.save(notificationEvent);

        webhookDeliveryPort.deliver(notificationEvent).join();
        notificationEvent.markCompleted();
        return notificationRepositoryPort.save(notificationEvent);
    }

    @SuppressWarnings("unused")
    @Recover
    public NotificationEvent recover(RuntimeException ex, NotificationEvent notificationEvent) {
        notificationEvent.setRetryCount(maxAttempts);
        notificationEvent.markFailed(ex.getMessage());
        notificationEvent.setNextAttemptAt(null);
        return notificationRepositoryPort.save(notificationEvent);
    }
}




