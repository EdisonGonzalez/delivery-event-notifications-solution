package com.delivery.event.notification.application.port.in;

import java.util.UUID;

/**
 * Use case for replaying failed notification events.
 */
public interface NotificationReplayUseCase {

    void replay(String clientId, UUID notificationEventId);
}

