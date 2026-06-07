package com.delivery.event.notification.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Extracts the authenticated client identifier from the security context. */
@Component
public class CurrentClientProvider {

  /**
   * Obtiene el clientId autenticado desde el contexto de seguridad.
   *
   * @return Identificador del cliente autenticado.
   */
  public String currentClientId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || authentication.getName() == null
        || authentication.getName().isBlank()) {
      throw new IllegalStateException("No authenticated client present");
    }
    return authentication.getName();
  }
}
