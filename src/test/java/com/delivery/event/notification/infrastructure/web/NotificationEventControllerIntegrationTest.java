package com.delivery.event.notification.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.delivery.event.notification.domain.model.NotificationEvent;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
// @Disabled("Requires Docker - enable in local/dev environment with Docker available")
class NotificationEventControllerIntegrationTest {

  @Container
  @SuppressWarnings("resource")
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("notifications")
          .withUsername("postgres")
          .withPassword("postgres");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.enabled", () -> "false");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    // Provide a test user that matches the seeded data client-id (client-a)
    registry.add("spring.security.user.name", () -> "client-a");
    registry.add("spring.security.user.password", () -> "password");
  }

  @LocalServerPort int port;

  @Autowired TestRestTemplate restTemplate;

  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedData() {
    jdbcTemplate.update("delete from notification_events");

    jdbcTemplate.update(
        """
                insert into notification_events (
                    id, event_id, event_type, client_id, content, webhook_url,
                    delivery_status, retry_count, reason, next_attempt_at,
                    created_by, created_at, updated_by, updated_at, channel, correlation_id, version
                ) values (
                    cast(? as uuid), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, ?, ?
                )
                """,
        "11111111-1111-1111-1111-111111111111",
        "evt-it-0001",
        "ORDER_CREATED",
        "client-a",
        "{\"orderId\":\"ORD-1001\"}",
        "https://client-a.example.com/webhooks/orders",
        "COMPLETED",
        0,
        null,
        null,
        "test",
        "test",
        "api",
        "corr-it-0001",
        0L);

    jdbcTemplate.update(
        """
                insert into notification_events (
                    id, event_id, event_type, client_id, content, webhook_url,
                    delivery_status, retry_count, reason, next_attempt_at,
                    created_by, created_at, updated_by, updated_at, channel, correlation_id, version
                ) values (
                    cast(? as uuid), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, ?, ?
                )
                """,
        "22222222-2222-2222-2222-222222222222",
        "evt-it-0002",
        "ORDER_CREATED",
        "client-b",
        "{\"orderId\":\"ORD-2002\"}",
        "https://client-b.example.com/webhooks/orders",
        "FAILED",
        1,
        "timeout",
        null,
        "test",
        "test",
        "api",
        "corr-it-0002",
        0L);

    jdbcTemplate.update(
        """
                insert into notification_events (
                    id, event_id, event_type, client_id, content, webhook_url,
                    delivery_status, retry_count, reason, next_attempt_at,
                    created_by, created_at, updated_by, updated_at, channel, correlation_id, version
                ) values (
                    cast(? as uuid), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, ?, ?
                )
                """,
        "33333333-3333-3333-3333-333333333333",
        "evt-it-0003",
        "ORDER_CREATED",
        "client-a",
        "{\"orderId\":\"ORD-3003\"}",
        "https://client-a.example.com/webhooks/orders",
        "FAILED",
        2,
        "gateway-timeout",
        null,
        "test",
        "test",
        "api",
        "corr-it-0003",
        0L);
  }

  @Test
  void shouldReturnEventsForAuthenticatedClient() {
    ResponseEntity<NotificationEvent[]> resp =
        restTemplate
            .withBasicAuth("client-a", "password")
            .getForEntity(
                "http://localhost:" + port + "/notification_events", NotificationEvent[].class);

    assertEquals(HttpStatus.OK, resp.getStatusCode());
    NotificationEvent[] body = resp.getBody();
    assertTrue(body != null && body.length >= 1);
    boolean found =
        Arrays.stream(body)
            .anyMatch(e -> "11111111-1111-1111-1111-111111111111".equals(e.getId().toString()));
    assertTrue(found, "Expected seeded event for client-a to be present");
  }

  @Test
  void shouldNotAllowAccessToOtherClientsEvents() {
    // client-a should not be able to fetch client-b event
    ResponseEntity<String> resp =
        restTemplate
            .withBasicAuth("client-a", "password")
            .getForEntity(
                "http://localhost:"
                    + port
                    + "/notification_events/22222222-2222-2222-2222-222222222222",
                String.class);

    assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
  }

  @Test
  void shouldReplayFailedEventForAuthenticatedClient() {
    ResponseEntity<Void> resp =
        restTemplate
            .withBasicAuth("client-a", "password")
            .postForEntity(
                "http://localhost:"
                    + port
                    + "/notification_events/33333333-3333-3333-3333-333333333333/replay",
                null,
                Void.class);

    assertEquals(HttpStatus.ACCEPTED, resp.getStatusCode());

    String status =
        jdbcTemplate.queryForObject(
            "select delivery_status from notification_events where id = cast(? as uuid)",
            String.class,
            "33333333-3333-3333-3333-333333333333");
    Integer retryCount =
        jdbcTemplate.queryForObject(
            "select retry_count from notification_events where id = cast(? as uuid)",
            Integer.class,
            "33333333-3333-3333-3333-333333333333");
    String reason =
        jdbcTemplate.queryForObject(
            "select reason from notification_events where id = cast(? as uuid)",
            String.class,
            "33333333-3333-3333-3333-333333333333");

    assertEquals("PENDING", status);
    assertEquals(0, retryCount);
    assertNull(reason);
  }

  @Test
  void shouldRejectReplayWhenEventIsNotFailed() {
    ResponseEntity<String> resp =
        restTemplate
            .withBasicAuth("client-a", "password")
            .postForEntity(
                "http://localhost:"
                    + port
                    + "/notification_events/11111111-1111-1111-1111-111111111111/replay",
                null,
                String.class);

    assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
  }
}
