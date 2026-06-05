package com.delivery.event.notification.application.service;

import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import com.delivery.event.notification.domain.exception.NotificationNotFoundException;
import com.delivery.event.notification.domain.exception.NotificationReplayException;
import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationReplayServiceTest {

    @Mock
    private NotificationRepositoryPort repositoryPort;

    @InjectMocks
    private NotificationReplayService service;

    @Test
    void shouldResetFailedNotificationToPending() {
        UUID id = UUID.randomUUID();
        NotificationEvent event = new NotificationEvent();
        event.setId(id);
        event.setDeliveryStatus(DeliveryStatus.FAILED);
        event.setRetryCount(2);

        when(repositoryPort.findByIdAndClientId(id, "CLIENT-1")).thenReturn(Optional.of(event));

        service.replay("CLIENT-1", id);

        assertThat(event.getDeliveryStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        verify(repositoryPort).save(event);
    }

    @Test
    void shouldRejectReplayForNonFailedNotifications() {
        UUID id = UUID.randomUUID();
        NotificationEvent event = new NotificationEvent();
        event.setDeliveryStatus(DeliveryStatus.COMPLETED);

        when(repositoryPort.findByIdAndClientId(id, "CLIENT-1")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.replay("CLIENT-1", id))
                .isInstanceOf(NotificationReplayException.class)
                .hasMessage("Only failed notifications can be replayed");
    }

    @Test
    void shouldThrowWhenNotificationDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repositoryPort.findByIdAndClientId(id, "CLIENT-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replay("CLIENT-1", id))
                .isInstanceOf(NotificationNotFoundException.class);
    }
}

