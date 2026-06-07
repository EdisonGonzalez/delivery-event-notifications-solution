package com.delivery.event.notification.infrastructure.persistence.jpa;

import com.delivery.event.notification.domain.model.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** JPA representation of a notification event. */
@Setter
@Getter
@Entity
@Table(name = "notification_events")
public class NotificationEventEntity extends AuditEntity {

  @Column(name = "event_id", nullable = false, unique = true, length = 36)
  private String eventId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "client_id", nullable = false)
  private String clientId;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "webhook_url", nullable = false, columnDefinition = "text")
  private String webhookUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "delivery_status", nullable = false)
  private DeliveryStatus deliveryStatus;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "reason", columnDefinition = "text")
  private String reason;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt;
}
