package com.delivery.event.notification.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.application.port.out.SubscriptionPort;
import com.delivery.event.notification.application.port.out.WebhookDeliveryPort;
import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import com.delivery.event.notification.domain.model.Subscription;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryProcessorTest {

  @Mock private NotificationRepositoryPort notificationRepositoryPort;

  @Mock private SubscriptionPort subscriptionPort;

  @Mock private WebhookDeliveryPort webhookDeliveryPort;

  private NotificationDeliveryProcessor processor;

  @BeforeEach
  void setUp() {
    processor =
        new NotificationDeliveryProcessor(
            notificationRepositoryPort, subscriptionPort, webhookDeliveryPort, 3);
  }

  @Test
  void shouldMarkIgnoredWhenSubscriptionIsMissing() {
    NotificationEvent event = baseEvent();
    when(subscriptionPort.findActiveSubscription("client-a", "ORDER_CREATED"))
        .thenReturn(Optional.empty());
    when(notificationRepositoryPort.save(any(NotificationEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    NotificationEvent result = processor.deliver(event);

    assertEquals(DeliveryStatus.IGNORED, result.getDeliveryStatus());
    assertEquals("No active subscription found", result.getReason());
    verify(notificationRepositoryPort).save(event);
  }

  @Test
  void shouldMarkCompletedWhenDeliverySucceeds() {
    NotificationEvent event = baseEvent();
    Subscription subscription = new Subscription();
    subscription.setClientId("client-a");
    subscription.setEventType("ORDER_CREATED");
    subscription.setActive(true);

    when(subscriptionPort.findActiveSubscription("client-a", "ORDER_CREATED"))
        .thenReturn(Optional.of(subscription));
    when(webhookDeliveryPort.deliver(event)).thenReturn(CompletableFuture.completedFuture(null));
    List<DeliveryStatus> savedStatuses = new ArrayList<>();
    when(notificationRepositoryPort.save(any(NotificationEvent.class)))
        .thenAnswer(
            invocation -> {
              NotificationEvent saved = invocation.getArgument(0);
              savedStatuses.add(saved.getDeliveryStatus());
              return saved;
            });

    NotificationEvent result = processor.deliver(event);

    assertEquals(DeliveryStatus.COMPLETED, result.getDeliveryStatus());
    assertEquals(List.of(DeliveryStatus.IN_PROGRESS, DeliveryStatus.COMPLETED), savedStatuses);
  }

  @Test
  void recoverShouldMarkEventAsFailed() {
    NotificationEvent event = baseEvent();
    when(notificationRepositoryPort.save(any(NotificationEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    NotificationEvent result = processor.recover(new RuntimeException("boom"), event);

    assertEquals(DeliveryStatus.FAILED, result.getDeliveryStatus());
    assertEquals(3, result.getRetryCount());
    assertEquals("boom", result.getReason());
    assertNull(result.getNextAttemptAt());
    verify(notificationRepositoryPort).save(event);
  }

  private NotificationEvent baseEvent() {
    NotificationEvent event = new NotificationEvent();
    event.setClientId("client-a");
    event.setEventType("ORDER_CREATED");
    event.setWebhookUrl("https://example.com/hook");
    event.setContent("{\"orderId\":\"ORD-1\"}");
    event.setDeliveryStatus(DeliveryStatus.PENDING);
    return event;
  }
}
