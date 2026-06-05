package com.delivery.event.notification.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditContextConfigTest {

    @Test
    void shouldReturnSystemAsDefaultAuditor() {
        AuditContextConfig config = new AuditContextConfig();

        assertThat(config.auditorAware().getCurrentAuditor())
                .hasValue("system");
    }
}

