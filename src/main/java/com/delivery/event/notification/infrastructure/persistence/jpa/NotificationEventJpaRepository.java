package com.delivery.event.notification.infrastructure.persistence.jpa;

import com.delivery.event.notification.domain.model.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for notification events.
 */
public interface NotificationEventJpaRepository extends JpaRepository<NotificationEventEntity, UUID> {

	Optional<NotificationEventEntity> findByIdAndClientId(UUID id, String clientId);

	@Query("""
			select e from NotificationEventEntity e
			where e.clientId = :clientId
			  and e.deliveryStatus = coalesce(:status, e.deliveryStatus)
			  and e.createdAt >= coalesce(:from, e.createdAt)
			  and e.createdAt <= coalesce(:to, e.createdAt)
			order by e.createdAt desc
			""")
	List<NotificationEventEntity> findAllByClientIdAndFilter(@Param("clientId") String clientId,
															 @Param("status") DeliveryStatus status,
															 @Param("from") Instant from,
															 @Param("to") Instant to);

	@Query(value = """
			select *
			from notification_events
			where delivery_status = 'PENDING'
			  and next_attempt_at <= now()
			order by created_at asc
			for update skip locked
			limit :batchSize
			""", nativeQuery = true)
	List<NotificationEventEntity> claimPendingBatch(@Param("batchSize") int batchSize);

	long countByDeliveryStatus(DeliveryStatus status);
}


