package com.delivery.event.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.domain.exception.NotificationNotFoundException;
import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

  @Mock private NotificationRepositoryPort repositoryPort;

  @InjectMocks private NotificationQueryService service;

  @Test
  void shouldReturnFilteredNotifications() {
    NotificationEvent event = new NotificationEvent();
    event.setEventId("EVT-1");
    when(repositoryPort.findAllByClientIdAndFilter(
            "CLIENT-1", DeliveryStatus.COMPLETED, null, null))
        .thenReturn(List.of(event));

    List<NotificationEvent> result =
        service.findAll("CLIENT-1", DeliveryStatus.COMPLETED, null, null);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getEventId()).isEqualTo("EVT-1");
  }

  @Test
  void shouldThrowWhenNotificationDoesNotBelongToClient() {
    UUID id = UUID.randomUUID();
    when(repositoryPort.findByIdAndClientId(id, "CLIENT-1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findById("CLIENT-1", id))
        .isInstanceOf(NotificationNotFoundException.class)
        .hasMessage("Notification event not found");
  }
}
