package com.delivery.event.notification.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessNotificationServiceTest {

  @Mock private NotificationRepositoryPort repositoryPort;

  @Mock private NotificationDeliveryProcessor notificationDeliveryProcessor;

  @InjectMocks private ProcessNotificationService service;

  @Test
  void shouldIgnoreNotificationWithoutSubscription() {
    NotificationEvent event = new NotificationEvent();
    event.setId(UUID.randomUUID());
    event.setClientId("CLIENT-1");
    event.setEventType("PAYMENT");
    event.setDeliveryStatus(DeliveryStatus.PENDING);

    when(repositoryPort.findClaimableBatch(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(List.of(event));
    when(notificationDeliveryProcessor.deliver(event)).thenReturn(event);

    service.processBatch();

    verify(notificationDeliveryProcessor).deliver(event);
  }

  @Test
  void shouldMarkNotificationCompletedAfterSuccessfulDelivery() {
    NotificationEvent event = new NotificationEvent();
    event.setId(UUID.randomUUID());
    event.setClientId("CLIENT-1");
    event.setEventType("PAYMENT");
    event.setDeliveryStatus(DeliveryStatus.PENDING);
    event.setRetryCount(0);

    when(repositoryPort.findClaimableBatch(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(List.of(event));
    when(notificationDeliveryProcessor.deliver(event)).thenReturn(event);

    service.processBatch();

    verify(notificationDeliveryProcessor).deliver(event);
  }

  @Test
  void shouldRetryFailedDelivery() {
    NotificationEvent event = new NotificationEvent();
    event.setId(UUID.randomUUID());
    event.setClientId("CLIENT-1");
    event.setEventType("PAYMENT");
    event.setDeliveryStatus(DeliveryStatus.PENDING);
    event.setRetryCount(0);
    event.setWebhookUrl("https://example.com/webhook");

    when(repositoryPort.findClaimableBatch(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(List.of(event));
    doThrow(new RuntimeException("boom")).when(notificationDeliveryProcessor).deliver(event);

    assertThatThrownBy(() -> service.processBatch())
        .isInstanceOf(RuntimeException.class)
        .hasMessage("boom");

    verify(notificationDeliveryProcessor).deliver(event);
  }
}
