package com.delivery.event.notification.application.port.out;

import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for notification events. */
public interface NotificationRepositoryPort {

  /** Lists events for a client using optional filters. */
  List<NotificationEvent> findAllByClientIdAndFilter(
      String clientId, DeliveryStatus status, Instant from, Instant to);

  /** Finds an event by id restricted to the authenticated client. */
  Optional<NotificationEvent> findByIdAndClientId(UUID notificationEventId, String clientId);

  /** Finds an event by id without a client filter (internal use). */
  Optional<NotificationEvent> findById(UUID notificationEventId);

  /** Claims pending events ready for concurrent processing. */
  List<NotificationEvent> findClaimableBatch(Instant now, int batchSize);

  /** Persists an event and returns its updated representation. */
  NotificationEvent save(NotificationEvent notificationEvent);

  /**
   * @return Current total of pending events.
   */
  long countPending();
}
