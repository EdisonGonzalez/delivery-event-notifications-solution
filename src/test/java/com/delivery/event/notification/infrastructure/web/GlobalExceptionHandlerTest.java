package com.delivery.event.notification.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.delivery.event.notification.domain.exception.NotificationNotFoundException;
import com.delivery.event.notification.domain.exception.NotificationReplayException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void shouldMapNotFoundExceptionTo404() {
    ResponseEntity<Map<String, Object>> response =
        handler.handleNotFound(new NotificationNotFoundException("not found"));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("not found", response.getBody().get("error"));
    assertNotNull(response.getBody().get("timestamp"));
  }

  @Test
  void shouldMapReplayExceptionTo409() {
    ResponseEntity<Map<String, Object>> response =
        handler.handleReplay(new NotificationReplayException("conflict"));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("conflict", response.getBody().get("error"));
    assertNotNull(response.getBody().get("timestamp"));
  }

  @Test
  void shouldMapIllegalArgumentExceptionTo400() {
    ResponseEntity<Map<String, Object>> response =
        handler.handleIllegalArgument(new IllegalArgumentException("bad request"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("bad request", response.getBody().get("error"));
    assertNotNull(response.getBody().get("timestamp"));
  }
}
