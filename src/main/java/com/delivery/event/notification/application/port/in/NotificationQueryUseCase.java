package com.delivery.event.notification.application.port.in;

import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Use case for querying notification events by the authenticated client. */
public interface NotificationQueryUseCase {

  /**
   * Finds all events by client id with optional filters.
   *
   * @param clientId id of the client
   * @param status notification state
   * @param from instant value for get the notifications from
   * @param to instant value for get the notifications to
   * @return NotificationEvent list
   */
  List<NotificationEvent> findAll(String clientId, DeliveryStatus status, Instant from, Instant to);

  /**
   * Find event by id
   *
   * @param clientId id of the client
   * @param notificationEventId notification event id to looking for
   * @return NotificationEvent element
   */
  NotificationEvent findById(String clientId, UUID notificationEventId);
}
