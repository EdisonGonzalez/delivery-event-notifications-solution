package com.delivery.event.notification.infrastructure.persistence.adapter;

import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import com.delivery.event.notification.infrastructure.persistence.jpa.NotificationEventJpaRepository;
import com.delivery.event.notification.infrastructure.persistence.mapper.NotificationEventJpaMapper;
import com.delivery.event.notification.infrastructure.web.dto.Page;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/** JPA-driven implementation of the notification repository port. */
@Repository
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

  private final NotificationEventJpaRepository repository;
  private final NotificationEventJpaMapper mapper;

  /**
   * @param repository JPA repository for notification events
   * @param mapper JPA mapper for notification events
   */
  public NotificationRepositoryAdapter(
      NotificationEventJpaRepository repository, NotificationEventJpaMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  /** {@inheritDoc} */
  @Override
  public Page<NotificationEvent> findAllByClientIdAndFilter(
      String clientId, DeliveryStatus status, Instant from, Instant to, int page, int size) {
    var springPage =
        repository.findAllByClientIdAndFilter(clientId, status, from, to, PageRequest.of(page, size));
    var content =
        springPage.getContent().stream().map(mapper::toDomain).toList();
    return Page.of(content, page, size, springPage.getTotalElements());
  }

  /** {@inheritDoc} */
  @Override
  public Optional<NotificationEvent> findByIdAndClientId(
      UUID notificationEventId, String clientId) {
    return repository
        .findByIdAndClientId(notificationEventId, clientId)
        .map(mapper::toDomain);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<NotificationEvent> findById(UUID notificationEventId) {
    return repository.findById(notificationEventId).map(mapper::toDomain);
  }

  /** {@inheritDoc} */
  @Override
  public List<NotificationEvent> findClaimableBatch(Instant now, int batchSize) {
    return repository.claimPendingBatch(batchSize).stream()
        .map(mapper::toDomain)
        .toList();
  }

  /** {@inheritDoc} */
  @Override
  public NotificationEvent save(NotificationEvent notificationEvent) {
    if (notificationEvent.getId() == null) {
      return mapper.toDomain(repository.save(mapper.toEntity(notificationEvent)));
    }

    var entityToPersist =
        repository
            .findById(notificationEvent.getId())
            .map(existing -> applyDeliveryState(existing, notificationEvent))
            .orElseGet(() -> mapper.toEntity(notificationEvent));

    return mapper.toDomain(repository.save(entityToPersist));
  }

  private com.delivery.event.notification.infrastructure.persistence.jpa.NotificationEventEntity
      applyDeliveryState(
          com.delivery.event.notification.infrastructure.persistence.jpa.NotificationEventEntity entity,
          NotificationEvent source) {
    entity.setDeliveryStatus(source.getDeliveryStatus());
    entity.setRetryCount(source.getRetryCount());
    entity.setReason(source.getReason());
    entity.setNextAttemptAt(source.getNextAttemptAt());

    if (source.getWebhookUrl() != null) {
      entity.setWebhookUrl(source.getWebhookUrl());
    }
    if (source.getContent() != null) {
      entity.setContent(source.getContent());
    }
    if (source.getChannel() != null) {
      entity.setChannel(source.getChannel());
    }
    if (source.getCorrelationId() != null) {
      entity.setCorrelationId(source.getCorrelationId());
    }
    return entity;
  }

  /** {@inheritDoc} */
  @Override
  public long countPending() {
    return repository.countByDeliveryStatus(DeliveryStatus.PENDING);
  }
}
