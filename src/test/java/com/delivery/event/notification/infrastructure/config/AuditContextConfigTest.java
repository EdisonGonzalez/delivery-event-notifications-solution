package com.delivery.event.notification.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditContextConfigTest {

  @Test
  void shouldReturnSystemAsDefaultAuditor() {
    AuditContextConfig config = new AuditContextConfig();

    assertThat(config.auditorAware().getCurrentAuditor()).hasValue("system");
  }
}
