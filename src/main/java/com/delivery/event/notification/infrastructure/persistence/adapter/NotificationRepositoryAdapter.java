package com.delivery.event.notification.infrastructure.persistence.adapter;

import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import com.delivery.event.notification.infrastructure.persistence.jpa.NotificationEventJpaRepository;
import com.delivery.event.notification.infrastructure.persistence.mapper.NotificationEventMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** JPA-driven implementation of the notification repository port. */
@Repository
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

  private final NotificationEventJpaRepository repository;

  /**
   * @param repository JPA repository for notification events
   */
  public NotificationRepositoryAdapter(NotificationEventJpaRepository repository) {
    this.repository = repository;
  }

  /** {@inheritDoc} */
  @Override
  public List<NotificationEvent> findAllByClientIdAndFilter(
      String clientId, DeliveryStatus status, Instant from, Instant to) {
    return repository.findAllByClientIdAndFilter(clientId, status, from, to).stream()
        .map(NotificationEventMapper::toDomain)
        .toList();
  }

  /** {@inheritDoc} */
  @Override
  public Optional<NotificationEvent> findByIdAndClientId(
      UUID notificationEventId, String clientId) {
    return repository
        .findByIdAndClientId(notificationEventId, clientId)
        .map(NotificationEventMapper::toDomain);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<NotificationEvent> findById(UUID notificationEventId) {
    return repository.findById(notificationEventId).map(NotificationEventMapper::toDomain);
  }

  /** {@inheritDoc} */
  @Override
  public List<NotificationEvent> findClaimableBatch(Instant now, int batchSize) {
    return repository.claimPendingBatch(batchSize).stream()
        .map(NotificationEventMapper::toDomain)
        .toList();
  }

  /** {@inheritDoc} */
  @Override
  public NotificationEvent save(NotificationEvent notificationEvent) {
    return NotificationEventMapper.toDomain(
        repository.save(NotificationEventMapper.toEntity(notificationEvent)));
  }

  /** {@inheritDoc} */
  @Override
  public long countPending() {
    return repository.countByDeliveryStatus(DeliveryStatus.PENDING);
  }
}
