package com.delivery.event.notification.domain.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Base audit model for every persisted aggregate. */
@Setter
@Getter
public abstract class Audit {
  private UUID id;
  private String createdBy;
  private Instant createdAt;
  private String updatedBy;
  private Instant updatedAt;
  private String channel;
  private String correlationId;
  private Long version;
}
