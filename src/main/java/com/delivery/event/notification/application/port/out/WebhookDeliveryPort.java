package com.delivery.event.notification.application.port.out;

import com.delivery.event.notification.domain.model.NotificationEvent;
import java.util.concurrent.CompletableFuture;

/** Outbound port for sending webhook notifications. */
public interface WebhookDeliveryPort {

  /**
   * Sends the event payload to the configured webhook.
   *
   * @param notificationEvent Event to deliver.
   * @return Future that completes when the delivery finishes.
   */
  CompletableFuture<Void> deliver(NotificationEvent notificationEvent);
}
