package com.delivery.event.notification.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

class WebhookSecurityServiceTest {

  private final WebhookSecurityService service = new WebhookSecurityService();

  @Test
  void shouldRejectNullUri() {
    assertThrows(IllegalArgumentException.class, () -> service.validate(null));
  }

  @Test
  void shouldRejectUnsupportedScheme() {
    URI ftpUri = URI.create("ftp://example.com/hook");
    assertThrows(IllegalArgumentException.class, () -> service.validate(ftpUri));
  }

  @Test
  void shouldRejectBlockedHostIgnoringCase() {
    URI localhostUri = URI.create("http://LOCALHOST/hook");
    assertThrows(IllegalArgumentException.class, () -> service.validate(localhostUri));
  }

  @Test
  void shouldRejectPrivateDestination() {
    URI privateIpUri = URI.create("http://192.168.1.10/hook");
    assertThrows(IllegalArgumentException.class, () -> service.validate(privateIpUri));
  }

  @Test
  void shouldRejectUnresolvableHost() {
    URI unresolvableUri = URI.create("https://no-host-1234567890.invalid/hook");
    assertThrows(IllegalArgumentException.class, () -> service.validate(unresolvableUri));
  }

  @Test
  void shouldAllowPublicIpDestination() {
    URI publicIpUri = URI.create("https://8.8.8.8/hook");
    assertDoesNotThrow(() -> service.validate(publicIpUri));
  }
}
