package com.delivery.event.notification.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduled outbox processing.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}

