package com.delivery.event.notification.infrastructure.web;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Protects outbound webhook dispatch against SSRF and DNS rebinding.
 */
@Component
public class WebhookSecurityService {

    private static final List<String> BLOCKED_HOSTS = List.of("localhost", "169.254.169.254");

    public void validate(URI uri) {
        if (uri == null || uri.getScheme() == null || !isAllowedScheme(uri.getScheme())) {
            throw new IllegalArgumentException("Only http and https URLs are allowed");
        }

        String host = uri.getHost();
        if (host == null || BLOCKED_HOSTS.contains(host.toLowerCase())) {
            throw new IllegalArgumentException("Blocked webhook destination");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress()) {
                    throw new IllegalArgumentException("Blocked private or loopback destination");
                }
            }
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("Webhook host could not be resolved", ex);
        }
    }

    private boolean isAllowedScheme(String scheme) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }
}

