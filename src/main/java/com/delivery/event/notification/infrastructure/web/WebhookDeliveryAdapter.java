package com.delivery.event.notification.infrastructure.web;

import com.delivery.event.notification.application.port.out.WebhookDeliveryPort;
import com.delivery.event.notification.domain.model.NotificationEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Sends webhook notifications asynchronously via WebClient.
 */
@Component
public class WebhookDeliveryAdapter implements WebhookDeliveryPort {

    private final WebClient webClient;
    private final WebhookSecurityService webhookSecurityService;

    public WebhookDeliveryAdapter(WebClient webClient, WebhookSecurityService webhookSecurityService) {
        this.webClient = webClient;
        this.webhookSecurityService = webhookSecurityService;
    }

    @Override
    @Async
    public CompletableFuture<Void> deliver(NotificationEvent notificationEvent) {
        URI target = URI.create(notificationEvent.getWebhookUrl());
        webhookSecurityService.validate(target);
        webClient.post()
                .uri(target)
                .bodyValue(notificationEvent.getContent())
                .retrieve()
                .toBodilessEntity()
                .block();
        return CompletableFuture.completedFuture(null);
    }
}

