package com.delivery.event.notification.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.delivery.event.notification.domain.model.DeliveryStatus;
import com.delivery.event.notification.domain.model.NotificationEvent;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for recover path with stale detached domain state.
 *
 * <p>This reproduces the scenario where a retry callback receives an old entity version and ensures
 * the recover operation persists FAILED state using the latest DB row.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class NotificationDeliveryProcessorIntegrationTest {

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
    registry.add("jwt.secret", () -> "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm");
  }

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private NotificationDeliveryProcessor processor;

  @BeforeEach
  void setup() {
    jdbcTemplate.update("delete from notification_events");
  }

  @Test
  void recoverShouldPersistFailedStateUsingLatestRowWhenInputIsStale() {
    UUID id = UUID.randomUUID();

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
        id.toString(),
        "evt-stale-001",
        "ORDER_CREATED",
        "client-b",
        "{\"orderId\":\"ORD-1\"}",
        "https://example.com/hook",
        "IN_PROGRESS",
        1,
        null,
        Timestamp.from(Instant.now()),
        "seed",
        "seed",
        "api",
        "corr-stale-001",
        1L);

    NotificationEvent stale = new NotificationEvent();
    stale.setId(id);
    stale.setVersion(0L);
    stale.setClientId("client-b");
    stale.setEventType("ORDER_CREATED");
    stale.setDeliveryStatus(DeliveryStatus.IN_PROGRESS);

    NotificationEvent recovered = processor.recover(new RuntimeException("boom"), stale);

    assertNotNull(recovered);
    assertEquals(DeliveryStatus.FAILED, recovered.getDeliveryStatus());
    assertEquals(3, recovered.getRetryCount());
    assertEquals("boom", recovered.getReason());

    String status =
        jdbcTemplate.queryForObject(
            "select delivery_status from notification_events where id = cast(? as uuid)",
            String.class,
            id.toString());
    Integer retryCount =
        jdbcTemplate.queryForObject(
            "select retry_count from notification_events where id = cast(? as uuid)",
            Integer.class,
            id.toString());

    assertEquals("FAILED", status);
    assertEquals(3, retryCount);
  }
}

