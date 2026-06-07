package com.delivery.event.notification.infrastructure.web;

import com.delivery.event.notification.application.port.in.NotificationQueryUseCase;
import com.delivery.event.notification.application.port.in.NotificationReplayUseCase;
import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import com.delivery.event.notification.infrastructure.security.CurrentClientProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for notification events. */
@RestController
@RequestMapping("/notification_events")
public class NotificationEventController {

  private final NotificationQueryUseCase notificationQueryUseCase;
  private final NotificationReplayUseCase notificationReplayUseCase;
  private final CurrentClientProvider currentClientProvider;

  /**
   * @param notificationQueryUseCase Caso de uso de consulta.
   * @param notificationReplayUseCase Caso de uso de replay.
   * @param currentClientProvider Proveedor del cliente autenticado.
   */
  public NotificationEventController(
      NotificationQueryUseCase notificationQueryUseCase,
      NotificationReplayUseCase notificationReplayUseCase,
      CurrentClientProvider currentClientProvider) {
    this.notificationQueryUseCase = notificationQueryUseCase;
    this.notificationReplayUseCase = notificationReplayUseCase;
    this.currentClientProvider = currentClientProvider;
  }

  /** Lista eventos del cliente autenticado con filtros opcionales. */
  @GetMapping
  public List<NotificationEvent> findAll(
      @RequestParam(required = false) DeliveryStatus deliveryStatus,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to) {
    return notificationQueryUseCase.findAll(
        currentClientProvider.currentClientId(), deliveryStatus, from, to);
  }

  /** Obtiene un evento por id para el cliente autenticado. */
  @GetMapping("/{notificationEventId}")
  public NotificationEvent findById(@PathVariable UUID notificationEventId) {
    return notificationQueryUseCase.findById(
        currentClientProvider.currentClientId(), notificationEventId);
  }

  /** Replays a failed event and returns HTTP 202 Accepted when the request is valid. */
  @PostMapping("/{notificationEventId}/replay")
  public ResponseEntity<Void> replay(@PathVariable UUID notificationEventId) {
    notificationReplayUseCase.replay(currentClientProvider.currentClientId(), notificationEventId);
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }
}
