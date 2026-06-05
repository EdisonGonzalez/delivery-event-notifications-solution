package com.delivery.event.notification.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for subscriptions.
 */
public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, UUID> {

	@Query("""
			select s from SubscriptionEntity s
			where s.clientId = :clientId
			  and s.eventType = :eventType
			  and s.active = true
			""")
	Optional<SubscriptionEntity> findActiveSubscription(@Param("clientId") String clientId,
														@Param("eventType") String eventType);
}

