package com.delivery.event.notification.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.delivery.event.notification.domain.model.NotificationEvent;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class WebhookDeliveryAdapterHttpServerTest {

  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void shouldSendPayloadToWebhookEndpoint() throws IOException {
    AtomicReference<String> bodyCapture = new AtomicReference<>("");
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/hook", exchange -> handleRequest(exchange, 200, bodyCapture));
    server.start();

    WebhookSecurityService securityService = mock(WebhookSecurityService.class);
    doNothing().when(securityService).validate(org.mockito.ArgumentMatchers.any());
    WebhookDeliveryAdapter adapter =
        new WebhookDeliveryAdapter(WebClient.builder().build(), securityService);

    NotificationEvent event = new NotificationEvent();
    event.setWebhookUrl("http://localhost:" + server.getAddress().getPort() + "/hook");
    event.setContent("{\"hello\":\"world\"}");

    CompletableFuture<Void> result = adapter.deliver(event);
    result.join();

    assertEquals("{\"hello\":\"world\"}", bodyCapture.get());
    verify(securityService).validate(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldPropagateErrorWhenWebhookRespondsWithServerError() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/hook-500", exchange -> handleRequest(exchange, 500, new AtomicReference<>("")));
    server.start();

    WebhookSecurityService securityService = mock(WebhookSecurityService.class);
    doNothing().when(securityService).validate(org.mockito.ArgumentMatchers.any());
    WebhookDeliveryAdapter adapter =
        new WebhookDeliveryAdapter(WebClient.builder().build(), securityService);

    NotificationEvent event = new NotificationEvent();
    event.setWebhookUrl("http://localhost:" + server.getAddress().getPort() + "/hook-500");
    event.setContent("{\"hello\":\"error\"}");

    assertThrows(RuntimeException.class, () -> adapter.deliver(event));
  }

  private void handleRequest(HttpExchange exchange, int status, AtomicReference<String> bodyCapture)
      throws IOException {
    try (InputStream requestBody = exchange.getRequestBody()) {
      bodyCapture.set(new String(requestBody.readAllBytes(), StandardCharsets.UTF_8));
    }
    byte[] responseBody = "ok".getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, responseBody.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(responseBody);
    }
  }
}
