package com.delivery.event.notification.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** JPA representation of a client subscription. */
@Setter
@Getter
@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity extends AuditEntity {

  @Column(name = "client_id", nullable = false)
  private String clientId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "target_url", nullable = false, columnDefinition = "text")
  private String targetUrl;

  @Column(name = "active", nullable = false)
  private boolean active;
}
