package com.delivery.event.notification.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    // JWT secret for testing
    registry.add(
        "jwt.secret", () -> "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm");
  }

  @LocalServerPort int port;

  @Autowired TestRestTemplate restTemplate;

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired PasswordEncoder passwordEncoder;

  @Autowired ObjectMapper objectMapper;

  private static final String USERNAME = "client-a";
  private static final String CLIENT_ID = "client-a";
  private static final String RAW_PASSWORD = "client-a-password";

  private String jwtToken;

  @BeforeEach
  void seedData() {
    jdbcTemplate.update("delete from notification_events");
    jdbcTemplate.update("delete from user_roles");
    jdbcTemplate.update("delete from users");

    // Insert test user
    jdbcTemplate.update(
        """
                insert into users (id, username, password, client_id, enabled, created_at, updated_at)
                values (cast(? as uuid), ?, ?, ?, ?, now(), now())
                """,
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        USERNAME,
        passwordEncoder.encode(RAW_PASSWORD),
        CLIENT_ID,
        true);
    jdbcTemplate.update(
        "insert into user_roles (user_id, role_name) values (cast(? as uuid), ?)",
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        "ROLE_CLIENT");

    jwtToken = authenticateAndGetToken();

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
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwtToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<Map> resp =
        restTemplate.exchange(
            "http://localhost:" + port + "/notification_events", HttpMethod.GET, entity, Map.class);

    assertEquals(HttpStatus.OK, resp.getStatusCode());
    Map<String, Object> body = resp.getBody();
    assertNotNull(body);
    assertTrue(body.containsKey("content"));
    assertTrue(body.containsKey("pageNumber"));
    assertTrue(body.containsKey("pageSize"));
    assertTrue(body.containsKey("totalElements"));
    assertTrue(body.containsKey("totalPages"));
  }

  @Test
  void shouldNotAllowAccessToOtherClientsEvents() {
    // client-a should not be able to fetch client-b event
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwtToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> resp =
        restTemplate.exchange(
            "http://localhost:"
                + port
                + "/notification_events/22222222-2222-2222-2222-222222222222",
            HttpMethod.GET,
            entity,
            String.class);

    assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
  }

  @Test
  void shouldReplayFailedEventForAuthenticatedClient() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwtToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<Void> resp =
        restTemplate.exchange(
            "http://localhost:"
                + port
                + "/notification_events/33333333-3333-3333-3333-333333333333/replay",
            HttpMethod.POST,
            entity,
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
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwtToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> resp =
        restTemplate.exchange(
            "http://localhost:"
                + port
                + "/notification_events/11111111-1111-1111-1111-111111111111/replay",
            HttpMethod.POST,
            entity,
            String.class);

    assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
  }

  @Test
  void shouldSupportPaginationWithPageAndSize() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwtToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<Map> firstPage =
        restTemplate.exchange(
            "http://localhost:" + port + "/notification_events?page=0&size=1",
            HttpMethod.GET,
            entity,
            Map.class);
    ResponseEntity<Map> secondPage =
        restTemplate.exchange(
            "http://localhost:" + port + "/notification_events?page=1&size=1",
            HttpMethod.GET,
            entity,
            Map.class);

    assertEquals(HttpStatus.OK, firstPage.getStatusCode());
    assertEquals(HttpStatus.OK, secondPage.getStatusCode());
    assertNotNull(firstPage.getBody());
    assertNotNull(secondPage.getBody());

    // Verification of pagination structure
    assertTrue(firstPage.getBody().containsKey("content"));
    assertTrue(firstPage.getBody().containsKey("pageSize"));
    int pageSize = ((Number) firstPage.getBody().get("pageSize")).intValue();
    assertEquals(1, pageSize);
  }

  @Test
  void shouldFilterBySnakeCaseDeliveryStatusParameter() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwtToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "http://localhost:" + port + "/notification_events?delivery_status=FAILED",
            HttpMethod.GET,
            entity,
            Map.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertTrue(body.containsKey("content"));
    assertTrue(body.containsKey("totalElements"));
  }

  private String authenticateAndGetToken() {
    LoginRequest request = new LoginRequest(USERNAME, RAW_PASSWORD);
    ResponseEntity<LoginResponse> response =
        restTemplate.postForEntity(
            "http://localhost:" + port + "/auth/login", request, LoginResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().token());
    return response.getBody().token();
  }

  private record LoginRequest(String username, String password) {}

  private record LoginResponse(String token) {}
}
