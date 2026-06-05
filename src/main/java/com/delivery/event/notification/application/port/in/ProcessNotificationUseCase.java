package com.delivery.event.notification.application.port.in;

import java.util.UUID;

/**
 * Use case for processing pending notifications.
 */
public interface ProcessNotificationUseCase {

    void processBatch();

    void processNotification(UUID notificationId);
}

