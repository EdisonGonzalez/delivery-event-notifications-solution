package com.delivery.event.notification.application.service;

import com.delivery.event.notification.application.port.in.CreateNotificationEventUseCase;
import com.delivery.event.notification.application.port.in.NotificationQueryUseCase;
import com.delivery.event.notification.application.port.in.NotificationReplayUseCase;
import com.delivery.event.notification.application.port.in.ProcessNotificationUseCase;
import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.application.port.out.SubscriptionPort;
import com.delivery.event.notification.application.port.out.WebhookDeliveryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Application service wiring. */
@Configuration
public class ApplicationServiceConfiguration {

  /**
   * @param notificationRepositoryPort Notification query persistence port.
   * @return Notification query use case implementation.
   */
  @Bean
  public NotificationQueryUseCase notificationQueryUseCase(
      NotificationRepositoryPort notificationRepositoryPort) {
    return new NotificationQueryService(notificationRepositoryPort);
  }

  /**
   * @param notificationRepositoryPort Notification persistence port.
   * @return Notification replay use case implementation.
   */
  @Bean
  public NotificationReplayUseCase notificationReplayUseCase(
      NotificationRepositoryPort notificationRepositoryPort) {
    return new NotificationReplayService(notificationRepositoryPort);
  }

  /**
   * @param notificationRepositoryPort Notification persistence port.
   * @return Create notification event use case implementation (development/testing only).
   */
  @Bean
  public CreateNotificationEventUseCase createNotificationEventUseCase(
      NotificationRepositoryPort notificationRepositoryPort) {
    return new CreateNotificationEventService(notificationRepositoryPort);
  }

  /**
   * @param notificationRepositoryPort Persistence port for delivery states.
   * @param subscriptionPort Port used to look up active subscriptions.
   * @param webhookDeliveryPort Webhook delivery port.
   * @param maxAttempts Maximum retry count.
   * @return Delivery processor with retry/backoff support.
   */
  @Bean
  public NotificationDeliveryProcessor notificationDeliveryProcessor(
      NotificationRepositoryPort notificationRepositoryPort,
      SubscriptionPort subscriptionPort,
      WebhookDeliveryPort webhookDeliveryPort,
      @Value("${notification.retry.max-attempts:3}") int maxAttempts) {
    return new NotificationDeliveryProcessor(
        notificationRepositoryPort, subscriptionPort, webhookDeliveryPort, maxAttempts);
  }

  /**
   * @param notificationRepositoryPort Persistence port used to claim the pending batch.
   * @param notificationDeliveryProcessor Event delivery component.
   * @return Use case that orchestrates outbox processing.
   */
  @Bean
  public ProcessNotificationUseCase processNotificationUseCase(
      NotificationRepositoryPort notificationRepositoryPort,
      NotificationDeliveryProcessor notificationDeliveryProcessor) {
    return new ProcessNotificationService(
        notificationRepositoryPort, notificationDeliveryProcessor);
  }
}
