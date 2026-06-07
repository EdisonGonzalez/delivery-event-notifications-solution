package com.delivery.event.notification.infrastructure.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

/** Supplies a simple auditor for createdBy/updatedBy fields. */
@Configuration
public class AuditContextConfig {

  /**
   * @return Auditor por defecto para campos createdBy/updatedBy.
   */
  @Bean
  public AuditorAware<String> auditorAware() {
    return () -> Optional.of("system");
  }
}
