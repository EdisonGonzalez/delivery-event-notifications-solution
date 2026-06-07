package com.delivery.event.notification.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating notification events (simulation only).
 *
 * @param eventId Event business identifier.
 * @param eventType Type of the event.
 * @param content Event content/payload in JSON format.
 * @param webhookUrl Target webhook URL for delivery.
 */
public record CreateNotificationEventRequestDto(
    @NotBlank(message = "eventId is required") String eventId,
    @NotBlank(message = "eventType is required") String eventType,
    @NotBlank(message = "content is required") String content,
    @NotBlank(message = "webhookUrl is required") String webhookUrl) {}

