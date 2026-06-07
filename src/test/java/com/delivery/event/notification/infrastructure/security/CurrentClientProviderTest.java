package com.delivery.event.notification.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.delivery.event.notification.infrastructure.config.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentClientProviderTest {

  private final CurrentClientProvider provider = new CurrentClientProvider();

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldReturnAuthenticatedClientId() {
    CustomUserDetails userDetails = new CustomUserDetails("client-a", "password", "client-a");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

    assertEquals("client-a", provider.currentClientId());
  }

  @Test
  void shouldRejectWhenAuthenticationIsMissing() {
    SecurityContextHolder.clearContext();

    assertThrows(IllegalStateException.class, provider::currentClientId);
  }

  @Test
  void shouldRejectWhenPrincipalIsNotCustomUserDetails() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("client-a", "n/a"));

    assertThrows(IllegalStateException.class, provider::currentClientId);
  }
}
