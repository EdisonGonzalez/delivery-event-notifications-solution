package com.delivery.event.notification.infrastructure.persistence.adapter;

import com.delivery.event.notification.application.port.out.SubscriptionPort;
import com.delivery.event.notification.domain.model.Subscription;
import com.delivery.event.notification.infrastructure.persistence.jpa.SubscriptionJpaRepository;
import com.delivery.event.notification.infrastructure.persistence.mapper.NotificationEventMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA-driven implementation of the subscription port.
 */
@Repository
public class SubscriptionRepositoryAdapter implements SubscriptionPort {

    private final SubscriptionJpaRepository repository;

    public SubscriptionRepositoryAdapter(SubscriptionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Subscription> findActiveSubscription(String clientId, String eventType) {
        return repository.findActiveSubscription(clientId, eventType)
                .map(NotificationEventMapper::toDomain);
    }
}

