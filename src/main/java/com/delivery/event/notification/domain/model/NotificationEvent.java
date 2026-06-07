package com.delivery.event.notification.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Domain object representing a notification event to be delivered. */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NotificationEvent extends Audit {

  private String eventId;
  private String eventType;
  private String clientId;
  private String content;
  private String webhookUrl;
  private DeliveryStatus deliveryStatus;
  private int retryCount;
  private String reason;
  private Instant nextAttemptAt;

  /** Reset the event to PENDING state in sense to allow the retry/reprocess */
  public void markPending() {
    this.deliveryStatus = DeliveryStatus.PENDING;
    this.retryCount = 0;
    this.reason = null;
  }

  /**
   * Marks the event as ignored.
   *
   * @param reason Functional reason for the discard.
   */
  public void markIgnored(String reason) {
    this.deliveryStatus = DeliveryStatus.IGNORED;
    this.reason = reason;
  }

  /**
   * Marks the event as failed.
   *
   * @param reason Technical or functional cause of the failure.
   */
  public void markFailed(String reason) {
    this.deliveryStatus = DeliveryStatus.FAILED;
    this.reason = reason;
  }

  /** Mark the event to COMPLETED and clean whatever reason */
  public void markCompleted() {
    this.deliveryStatus = DeliveryStatus.COMPLETED;
    this.reason = null;
  }
}
