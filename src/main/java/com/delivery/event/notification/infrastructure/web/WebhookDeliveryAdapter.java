package com.delivery.event.notification.infrastructure.web;

import com.delivery.event.notification.application.port.out.WebhookDeliveryPort;
import com.delivery.event.notification.domain.model.NotificationEvent;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Sends webhook notifications asynchronously via WebClient. */
@Component
public class WebhookDeliveryAdapter implements WebhookDeliveryPort {

  private final WebClient webClient;
  private final WebhookSecurityService webhookSecurityService;

  /**
   * @param webClient Reactive HTTP client configured for outbound webhook calls.
   * @param webhookSecurityService Anti-SSRF validation service.
   */
  public WebhookDeliveryAdapter(
      WebClient webClient, WebhookSecurityService webhookSecurityService) {
    this.webClient = webClient;
    this.webhookSecurityService = webhookSecurityService;
  }

  /** {@inheritDoc} */
  @Override
  @Async
  public CompletableFuture<Void> deliver(NotificationEvent notificationEvent) {
    URI target = URI.create(notificationEvent.getWebhookUrl());
    webhookSecurityService.validate(target);
    webClient
        .post()
        .uri(target)
        .bodyValue(notificationEvent.getContent())
        .retrieve()
        .toBodilessEntity()
        .block();
    return CompletableFuture.completedFuture(null);
  }
}
