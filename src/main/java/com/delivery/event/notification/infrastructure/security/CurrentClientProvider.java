package com.delivery.event.notification.infrastructure.security;

import com.delivery.event.notification.infrastructure.config.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Extracts the authenticated client identifier from the security context. */
@Component
public class CurrentClientProvider {

  /**
   * Retrieves the client identifier of the currently authenticated user from the security context.
   *
   * @return The client identifier of the authenticated user.
   */
  public String currentClientId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || authentication.getPrincipal() == null
        || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
      throw new IllegalStateException("No authenticated client present");
    }
    return userDetails.getClientId();
  }
}
