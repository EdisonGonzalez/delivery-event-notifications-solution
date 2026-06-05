package com.delivery.event.notification.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebhookSecurityServiceTest {

    private final WebhookSecurityService service = new WebhookSecurityService();

    @Test
    void shouldRejectNullUri() {
        assertThrows(IllegalArgumentException.class, () -> service.validate(null));
    }

    @Test
    void shouldRejectUnsupportedScheme() {
        assertThrows(IllegalArgumentException.class, () -> service.validate(URI.create("ftp://example.com/hook")));
    }

    @Test
    void shouldRejectBlockedHostIgnoringCase() {
        assertThrows(IllegalArgumentException.class, () -> service.validate(URI.create("http://LOCALHOST/hook")));
    }

    @Test
    void shouldRejectPrivateDestination() {
        assertThrows(IllegalArgumentException.class, () -> service.validate(URI.create("http://192.168.1.10/hook")));
    }

    @Test
    void shouldRejectUnresolvableHost() {
        assertThrows(IllegalArgumentException.class, () -> service.validate(URI.create("https://no-host-1234567890.invalid/hook")));
    }

    @Test
    void shouldAllowPublicIpDestination() {
        assertDoesNotThrow(() -> service.validate(URI.create("https://8.8.8.8/hook")));
    }
}

