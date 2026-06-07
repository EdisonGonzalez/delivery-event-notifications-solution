package com.delivery.event.notification.infrastructure.persistence.jpa;

import com.delivery.event.notification.domain.model.DeliveryStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for notification events. */
public interface NotificationEventJpaRepository
    extends JpaRepository<NotificationEventEntity, UUID> {

  /** Search an event by restricted id to the owner client */
  Optional<NotificationEventEntity> findByIdAndClientId(UUID id, String clientId);

  /** Find all events by client id with optional filters. */
  @Query(
      """
			select e from NotificationEventEntity e
			where e.clientId = :clientId
			  and e.deliveryStatus = coalesce(:status, e.deliveryStatus)
			  and e.createdAt >= coalesce(:from, e.createdAt)
			  and e.createdAt <= coalesce(:to, e.createdAt)
			order by e.createdAt desc
			""")
  Page<NotificationEventEntity> findAllByClientIdAndFilter(
      @Param("clientId") String clientId,
      @Param("status") DeliveryStatus status,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);

  /** Claims a batch of pending events using pessimistic non-blocking lock. */
  @Query(
      value =
          """
			SELECT *
			FROM notification_events
			WHERE delivery_status = 'PENDING'
			  AND next_attempt_at <= now()
			ORDER BY created_at ASC
			FOR UPDATE SKIP locked
			limit :batchSize
			""",
      nativeQuery = true)
  List<NotificationEventEntity> claimPendingBatch(@Param("batchSize") int batchSize);

  /** Counts events by delivery status. */
  long countByDeliveryStatus(DeliveryStatus status);
}
