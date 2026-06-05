package com.delivery.event.notification.application.service;

import com.delivery.event.notification.application.port.in.NotificationQueryUseCase;
import com.delivery.event.notification.application.port.in.NotificationReplayUseCase;
import com.delivery.event.notification.application.port.in.ProcessNotificationUseCase;
import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.application.port.out.SubscriptionPort;
import com.delivery.event.notification.application.port.out.WebhookDeliveryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

/**
 * Application service wiring.
 */
@Configuration
public class ApplicationServiceConfiguration {

    @Bean
    public NotificationQueryUseCase notificationQueryUseCase(NotificationRepositoryPort notificationRepositoryPort) {
        return new NotificationQueryService(notificationRepositoryPort);
    }

    @Bean
    public NotificationReplayUseCase notificationReplayUseCase(NotificationRepositoryPort notificationRepositoryPort) {
        return new NotificationReplayService(notificationRepositoryPort);
    }

    @Bean
    public NotificationDeliveryProcessor notificationDeliveryProcessor(NotificationRepositoryPort notificationRepositoryPort,
                                                                        SubscriptionPort subscriptionPort,
                                                                        WebhookDeliveryPort webhookDeliveryPort,
                                                                        @Value("${notification.retry.max-attempts:3}") int maxAttempts) {
        return new NotificationDeliveryProcessor(notificationRepositoryPort, subscriptionPort, webhookDeliveryPort, maxAttempts);
    }

    @Bean
    public ProcessNotificationUseCase processNotificationUseCase(NotificationRepositoryPort notificationRepositoryPort,
                                                                 NotificationDeliveryProcessor notificationDeliveryProcessor) {
        return new ProcessNotificationService(notificationRepositoryPort, notificationDeliveryProcessor);
    }
}

