package com.delivery.event.notification.application.port.out;

import com.delivery.event.notification.domain.model.Subscription;

import java.util.Optional;

/**
 * Persistence port for subscriptions.
 */
public interface SubscriptionPort {

    Optional<Subscription> findActiveSubscription(String clientId, String eventType);
}

