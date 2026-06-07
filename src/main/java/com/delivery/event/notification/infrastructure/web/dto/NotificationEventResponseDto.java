package com.delivery.event.notification.infrastructure.web.dto;

import com.delivery.event.notification.domain.model.DeliveryStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for notification events.
 *
 * @param id Event identifier.
 * @param eventId Event business identifier.
 * @param eventType Type of the event.
 * @param clientId Client identifier.
 * @param content Event content/payload.
 * @param webhookUrl Target webhook URL.
 * @param deliveryStatus Current delivery status.
 * @param retryCount Number of retry attempts.
 * @param reason Failure or ignore reason.
 * @param nextAttemptAt Next scheduled attempt time.
 * @param createdBy User who created the event.
 * @param createdAt Creation timestamp.
 * @param updatedBy User who last updated the event.
 * @param updatedAt Last update timestamp.
 */
public record NotificationEventResponseDto(
    UUID id,
    String eventId,
    String eventType,
    String clientId,
    String content,
    String webhookUrl,
    DeliveryStatus deliveryStatus,
    int retryCount,
    String reason,
    Instant nextAttemptAt,
    String createdBy,
    Instant createdAt,
    String updatedBy,
    Instant updatedAt) {}

