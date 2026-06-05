package com.delivery.event.notification.infrastructure.persistence.adapter;

import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import com.delivery.event.notification.infrastructure.persistence.jpa.NotificationEventJpaRepository;
import com.delivery.event.notification.infrastructure.persistence.mapper.NotificationEventMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-driven implementation of the notification repository port.
 */
@Repository
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationEventJpaRepository repository;

    public NotificationRepositoryAdapter(NotificationEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<NotificationEvent> findAllByClientIdAndFilter(String clientId, DeliveryStatus status, Instant from, Instant to) {
        return repository.findAllByClientIdAndFilter(clientId, status, from, to)
                .stream()
                .map(NotificationEventMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<NotificationEvent> findByIdAndClientId(UUID notificationEventId, String clientId) {
        return repository.findByIdAndClientId(notificationEventId, clientId)
                .map(NotificationEventMapper::toDomain);
    }

    @Override
    public Optional<NotificationEvent> findById(UUID notificationEventId) {
        return repository.findById(notificationEventId).map(NotificationEventMapper::toDomain);
    }

    @Override
    public List<NotificationEvent> findClaimableBatch(Instant now, int batchSize) {
        return repository.claimPendingBatch(batchSize)
                .stream()
                .map(NotificationEventMapper::toDomain)
                .toList();
    }

    @Override
    public NotificationEvent save(NotificationEvent notificationEvent) {
        return NotificationEventMapper.toDomain(repository.save(NotificationEventMapper.toEntity(notificationEvent)));
    }

    @Override
    public long countPending() {
        return repository.countByDeliveryStatus(com.delivery.event.notification.domain.model.DeliveryStatus.PENDING);
    }
}

