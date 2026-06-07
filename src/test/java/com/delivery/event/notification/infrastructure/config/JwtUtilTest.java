package com.delivery.event.notification.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for JwtUtil. */
class JwtUtilTest {

  private JwtUtil jwtUtil;

  @BeforeEach
  void setUp() throws Exception {
    jwtUtil = new JwtUtil();
    // Set a test secret key (must be at least 256 bits for HS256)
    setField(jwtUtil, "jwtSecret", "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm");
    setField(jwtUtil, "jwtExpirationMs", 86400000L); // 24 hours
  }

  @Test
  void generateToken_shouldCreateValidToken() {
    String token = jwtUtil.generateToken("testuser", "client-a");
    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void extractUsername_shouldReturnCorrectUsername() {
    String username = "testuser";
    String clientId = "client-a";
    String token = jwtUtil.generateToken(username, clientId);
    String extractedUsername = jwtUtil.extractUsername(token);
    assertEquals(username, extractedUsername);
  }

  @Test
  void extractClientId_shouldReturnCorrectClientId() {
    String username = "testuser";
    String clientId = "client-a";
    String token = jwtUtil.generateToken(username, clientId);
    String extractedClientId = jwtUtil.extractClientId(token);
    assertEquals(clientId, extractedClientId);
  }

  @Test
  void validateToken_shouldReturnTrueForValidToken() {
    String token = jwtUtil.generateToken("testuser", "client-a");
    assertTrue(jwtUtil.validateToken(token));
  }

  @Test
  void validateToken_shouldReturnFalseForInvalidToken() {
    assertFalse(jwtUtil.validateToken("invalid.token.here"));
  }

  @Test
  void validateToken_shouldReturnFalseForNullToken() {
    assertFalse(jwtUtil.validateToken(null));
  }

  @Test
  void generateToken_shouldIncludeClientIdClaim() {
    String username = "testuser";
    String clientId = "client-b";
    String token = jwtUtil.generateToken(username, clientId);
    String extractedClientId = jwtUtil.extractClientId(token);
    assertEquals(clientId, extractedClientId);
  }

  @Test
  void generateToken_shouldIncludeAuthoritiesClaim() {
    String token = jwtUtil.generateToken("testuser", "client-a", List.of("ROLE_CLIENT", "ROLE_ADMIN"));

    assertEquals(List.of("ROLE_CLIENT", "ROLE_ADMIN"), jwtUtil.extractAuthorities(token));
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
