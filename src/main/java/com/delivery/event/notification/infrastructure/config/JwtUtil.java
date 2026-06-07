package com.delivery.event.notification.infrastructure.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Collection;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Utility class for JWT token generation and validation. */
@Component
public class JwtUtil {

  @Value("${jwt.secret:your-secret-key-must-be-at-least-256-bits-long-for-hs256}")
  private String jwtSecret;

  @Value("${jwt.expiration:86400000}")
  private long jwtExpirationMs;

  private SecretKey getSigningKey() {
    byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * Generate JWT token for a given username and clientId.
   *
   * @param username the username
   * @param clientId the clientId for BOLA protection
   * @return JWT token string
   */
  public String generateToken(String username, String clientId) {
    return generateToken(username, clientId, List.of());
  }

  /**
   * Generate JWT token including client and authorities claims.
   *
   * @param username the username
   * @param clientId the clientId for BOLA protection
   * @param authorities authority names (e.g., ROLE_CLIENT, ROLE_ADMIN)
   * @return JWT token string
   */
  public String generateToken(String username, String clientId, Collection<String> authorities) {
    return Jwts.builder()
        .subject(username)
        .claim("clientId", clientId)
        .claim("authorities", authorities == null ? List.of() : List.copyOf(authorities))
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * Extract username from JWT token.
   *
   * @param token the JWT token
   * @return username
   */
  public String extractUsername(String token) {
    return getClaims(token).getSubject();
  }

  /**
   * Extract clientId from JWT token.
   *
   * @param token the JWT token
   * @return clientId
   */
  public String extractClientId(String token) {
    return getClaims(token).get("clientId", String.class);
  }

  /** Extract authority names from JWT token. */
  public List<String> extractAuthorities(String token) {
    Object raw = getClaims(token).get("authorities");
    if (!(raw instanceof Collection<?> rawAuthorities)) {
      return List.of();
    }

    return rawAuthorities.stream().map(String::valueOf).filter(value -> !value.isBlank()).toList();
  }

  /**
   * Validate JWT token.
   *
   * @param token the JWT token
   * @return true if valid, false otherwise
   */
  public boolean validateToken(String token) {
    try {
      getClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private Claims getClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }
}
