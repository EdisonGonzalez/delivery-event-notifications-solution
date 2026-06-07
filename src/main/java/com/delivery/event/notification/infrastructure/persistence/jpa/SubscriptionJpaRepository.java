package com.delivery.event.notification.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for subscriptions. */
public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, UUID> {

  /**
   * find an active subscription by client id and event type
   *
   * @param clientId id of the client
   * @param eventType type of the event
   * @return an active subscription
   */
  @Query(
      """
			SELECT s FROM SubscriptionEntity s
			WHERE s.clientId = :clientId
			  AND s.eventType = :eventType
			  AND s.active = TRUE
			""")
  Optional<SubscriptionEntity> findActiveSubscription(
      @Param("clientId") String clientId, @Param("eventType") String eventType);
}
