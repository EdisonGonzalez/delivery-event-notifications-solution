package com.delivery.event.notification.infrastructure.metrics;

import com.delivery.event.notification.application.port.out.NotificationRepositoryPort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Queue health metrics. */
@Component
public class NotificationMetrics {

  /**
   * Registers notification queue health metrics.
   *
   * @param meterRegistry Application metrics registry.
   * @param repositoryPort Port used to inspect the pending queue.
   */
  public NotificationMetrics(
      MeterRegistry meterRegistry, NotificationRepositoryPort repositoryPort) {
    Gauge.builder(
            "notifications.queue.pending.size",
            repositoryPort,
            NotificationRepositoryPort::countPending)
        .register(meterRegistry);
  }
}
