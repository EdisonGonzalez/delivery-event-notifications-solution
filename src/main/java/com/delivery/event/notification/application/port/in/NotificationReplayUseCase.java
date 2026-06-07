package com.delivery.event.notification.application.port.in;

import java.util.UUID;

/** Use case for replaying failed notification events. */
public interface NotificationReplayUseCase {

  /**
   * Requests reprocessing of a previously failed notification.
   *
   * @param clientId Authenticated client who owns the event.
   * @param notificationEventId Identifier of the event to replay.
   */
  void replay(String clientId, UUID notificationEventId);
}
