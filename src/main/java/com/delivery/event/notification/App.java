package com.delivery.event.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Main application entry point for the notification service.
 */
@SpringBootApplication
@EnableRetry
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

