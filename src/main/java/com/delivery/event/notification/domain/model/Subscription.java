package com.delivery.event.notification.domain.model;

import lombok.Getter;
import lombok.Setter;

/** Domain object describing an active subscription for a client and event type. */
@Setter
@Getter
public class Subscription extends Audit {

  private String clientId;
  private String eventType;
  private String targetUrl;
  private boolean active;
}
