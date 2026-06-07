package com.delivery.event.notification.infrastructure.web.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Login payload for username/password authentication. */
public record AuthLoginRequest(@NotBlank String username, @NotBlank String password) {}

