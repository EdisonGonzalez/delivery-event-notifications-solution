package com.delivery.event.notification.domain.model;

/** Possible notification delivery states. */
public enum DeliveryStatus {
  PENDING,
  IN_PROGRESS,
  COMPLETED,
  FAILED,
  IGNORED
}
