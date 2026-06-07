package com.delivery.event.notification.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.delivery.event.notification.infrastructure.config.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
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
class AuthControllerIntegrationTest {

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

  private static final String USERNAME = "client-auth";
  private static final String CLIENT_ID = "client-auth";
  private static final String RAW_PASSWORD = "client-auth-password";

  @LocalServerPort int port;

  @Autowired TestRestTemplate restTemplate;

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired PasswordEncoder passwordEncoder;

  @Autowired JwtUtil jwtUtil;

  @BeforeEach
  void seedData() {
    jdbcTemplate.update("delete from user_roles");
    jdbcTemplate.update("delete from users");

    jdbcTemplate.update(
        """
                insert into users (id, username, password, client_id, enabled, created_at, updated_at)
                values (cast(? as uuid), ?, ?, ?, ?, now(), now())
                """,
        "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
        USERNAME,
        passwordEncoder.encode(RAW_PASSWORD),
        CLIENT_ID,
        true);
    jdbcTemplate.update(
        "insert into user_roles (user_id, role_name) values (cast(? as uuid), ?)",
        "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
        "ROLE_CLIENT");
  }

  @Test
  void shouldAuthenticateExistingUserFromDatabase() {
    LoginRequest request = new LoginRequest(USERNAME, RAW_PASSWORD);

    ResponseEntity<LoginResponse> response =
        restTemplate.postForEntity("http://localhost:" + port + "/auth/login", request, LoginResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().token());
    assertTrue(jwtUtil.validateToken(response.getBody().token()));
    assertEquals(USERNAME, jwtUtil.extractUsername(response.getBody().token()));
    assertEquals(CLIENT_ID, jwtUtil.extractClientId(response.getBody().token()));
    assertTrue(jwtUtil.extractAuthorities(response.getBody().token()).contains("ROLE_CLIENT"));
  }

  @Test
  void shouldReturnUnauthorizedWhenPasswordIsInvalid() {
    LoginRequest request = new LoginRequest(USERNAME, "wrong-password");

    ResponseEntity<ErrorResponse> response =
        restTemplate.postForEntity(
            "http://localhost:" + port + "/auth/login", request, ErrorResponse.class);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Invalid credentials", response.getBody().error());
  }

  private record LoginRequest(String username, String password) {}

  private record LoginResponse(String token) {}

  private record ErrorResponse(String timestamp, String error) {}
}

