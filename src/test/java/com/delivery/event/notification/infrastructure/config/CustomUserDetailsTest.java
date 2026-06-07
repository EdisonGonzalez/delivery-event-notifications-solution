package com.delivery.event.notification.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/** Unit tests for CustomUserDetails. */
class CustomUserDetailsTest {

  @Test
  void constructor_shouldCreateValidUserDetails() {
    CustomUserDetails userDetails = new CustomUserDetails("testuser", "password", "client-a");
    assertEquals("testuser", userDetails.getUsername());
    assertEquals("password", userDetails.getPassword());
    assertEquals("client-a", userDetails.getClientId());
  }

  @Test
  void getAuthorities_shouldReturnRoleClient() {
    CustomUserDetails userDetails = new CustomUserDetails("testuser", "password", "client-a");
    Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
    assertEquals(1, authorities.size());
    assertTrue(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_CLIENT")));
  }

  @Test
  void isAccountNonExpired_shouldReturnTrue() {
    CustomUserDetails userDetails = new CustomUserDetails("testuser", "password", "client-a");
    assertTrue(userDetails.isAccountNonExpired());
  }

  @Test
  void isAccountNonLocked_shouldReturnTrue() {
    CustomUserDetails userDetails = new CustomUserDetails("testuser", "password", "client-a");
    assertTrue(userDetails.isAccountNonLocked());
  }

  @Test
  void isCredentialsNonExpired_shouldReturnTrue() {
    CustomUserDetails userDetails = new CustomUserDetails("testuser", "password", "client-a");
    assertTrue(userDetails.isCredentialsNonExpired());
  }

  @Test
  void isEnabled_shouldReturnTrue() {
    CustomUserDetails userDetails = new CustomUserDetails("testuser", "password", "client-a");
    assertTrue(userDetails.isEnabled());
  }

  @Test
  void getClientId_shouldReturnCorrectClientId() {
    String clientId = "client-b";
    CustomUserDetails userDetails = new CustomUserDetails("testuser", "password", clientId);
    assertEquals(clientId, userDetails.getClientId());
  }

  @Test
  void constructor_shouldUseProvidedAuthorities() {
    CustomUserDetails userDetails =
        new CustomUserDetails("testuser", "password", "client-a", List.of("ROLE_ADMIN"));

    assertEquals(1, userDetails.getAuthorities().size());
    assertTrue(
        userDetails.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
  }
}
