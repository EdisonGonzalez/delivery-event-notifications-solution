package com.delivery.event.notification.infrastructure.web;

import com.delivery.event.notification.application.port.in.CreateNotificationEventUseCase;
import com.delivery.event.notification.application.port.in.NotificationQueryUseCase;
import com.delivery.event.notification.application.port.in.NotificationReplayUseCase;
import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.infrastructure.security.CurrentClientProvider;
import com.delivery.event.notification.infrastructure.web.dto.CreateNotificationEventRequestDto;
import com.delivery.event.notification.infrastructure.web.dto.NotificationEventResponseDto;
import com.delivery.event.notification.infrastructure.web.dto.PageDto;
import com.delivery.event.notification.infrastructure.web.mapper.NotificationEventResponseDtoMapper;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for notification events. */
@RestController
@RequestMapping("/notification_events")
@PreAuthorize("hasRole('CLIENT')")
public class NotificationEventController {

  private final NotificationQueryUseCase notificationQueryUseCase;
  private final NotificationReplayUseCase notificationReplayUseCase;
  private final CreateNotificationEventUseCase createNotificationEventUseCase;
  private final CurrentClientProvider currentClientProvider;
  private final NotificationEventResponseDtoMapper mapper;

  /**
   * @param notificationQueryUseCase Caso de uso de consulta.
   * @param notificationReplayUseCase Caso de uso de replay.
   * @param createNotificationEventUseCase Caso de uso de creación (desarrollo).
   * @param currentClientProvider Proveedor del cliente autenticado.
   * @param mapper Mapper para convertir entre DTOs y objetos de dominio.
   */
  public NotificationEventController(
      NotificationQueryUseCase notificationQueryUseCase,
      NotificationReplayUseCase notificationReplayUseCase,
      CreateNotificationEventUseCase createNotificationEventUseCase,
      CurrentClientProvider currentClientProvider,
      NotificationEventResponseDtoMapper mapper) {
    this.notificationQueryUseCase = notificationQueryUseCase;
    this.notificationReplayUseCase = notificationReplayUseCase;
    this.createNotificationEventUseCase = createNotificationEventUseCase;
    this.currentClientProvider = currentClientProvider;
    this.mapper = mapper;
  }

  /** Lista eventos del cliente autenticado con filtros opcionales. */
  @GetMapping
  public PageDto<NotificationEventResponseDto> findAll(
      @RequestParam(name = "delivery_status", required = false) DeliveryStatus deliveryStatus,
      @RequestParam(name = "deliveryStatus", required = false) DeliveryStatus deliveryStatusCamel,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    if (page < 0 || size <= 0 || size > 100) {
      throw new IllegalArgumentException(
          "Invalid pagination parameters: page must be >= 0 and size between 1 and 100");
    }

    DeliveryStatus resolvedStatus = deliveryStatus != null ? deliveryStatus : deliveryStatusCamel;
    var pageResult =
        notificationQueryUseCase.findAll(
            currentClientProvider.currentClientId(), resolvedStatus, from, to, page, size);

    var dtoContent = pageResult.content().stream().map(mapper::toResponseDto).toList();

    return new PageDto<>(
        dtoContent,
        pageResult.pageNumber(),
        pageResult.pageSize(),
        pageResult.totalElements(),
        pageResult.totalPages(),
        pageResult.hasNextPage(),
        pageResult.hasPreviousPage());
  }

  /** Obtiene un evento por id para el cliente autenticado. */
  @GetMapping("/{notificationEventId}")
  public NotificationEventResponseDto findById(@PathVariable UUID notificationEventId) {
    var event =
        notificationQueryUseCase.findById(
            currentClientProvider.currentClientId(), notificationEventId);
    return mapper.toResponseDto(event);
  }

  /** Replays a failed event and returns HTTP 202 Accepted when the request is valid. */
  @PostMapping("/{notificationEventId}/replay")
  public ResponseEntity<Void> replay(@PathVariable UUID notificationEventId) {
    notificationReplayUseCase.replay(currentClientProvider.currentClientId(), notificationEventId);
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

  /**
   * Simulates the arrival of a new notification event (development only). Creates a PENDING event
   * ready for delivery.
   *
   * @param request The event creation request.
   * @return The created event response with HTTP 201 Created.
   */
  @PostMapping
  public ResponseEntity<NotificationEventResponseDto> create(
      @Valid @RequestBody CreateNotificationEventRequestDto request) {
    var createdEvent =
        createNotificationEventUseCase.create(
            currentClientProvider.currentClientId(),
            request.eventId(),
            request.eventType(),
            request.content(),
            request.webhookUrl());

    var responseDto = mapper.toResponseDto(createdEvent);
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
  }
}
