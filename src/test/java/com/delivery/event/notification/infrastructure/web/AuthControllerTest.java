package com.delivery.event.notification.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.delivery.event.notification.infrastructure.config.CustomUserDetails;
import com.delivery.event.notification.infrastructure.config.CustomUserDetailsService;
import com.delivery.event.notification.infrastructure.config.JwtUtil;
import com.delivery.event.notification.infrastructure.web.dto.auth.AuthLoginRequest;
import com.delivery.event.notification.infrastructure.web.dto.auth.AuthLoginResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/** Unit tests for AuthController. */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private AuthenticationManager authenticationManager;

  @Mock private CustomUserDetailsService userDetailsService;

  @Mock private JwtUtil jwtUtil;

  @InjectMocks private AuthController authController;

  @Mock private Authentication authentication;

  @BeforeEach
  void setUp() {
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
  }

  @Test
  void login_shouldReturnJwtToken() {
    String username = "testuser";
    String password = "password";
    String clientId = "client-a";
    String expectedToken = "jwt.token.here";

    CustomUserDetails userDetails = new CustomUserDetails(username, password, clientId);
    when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
    when(jwtUtil.generateToken(username, clientId, List.of("ROLE_CLIENT"))).thenReturn(expectedToken);

    AuthLoginRequest request = new AuthLoginRequest(username, password);
    ResponseEntity<AuthLoginResponse> response = authController.login(request);

    assertEquals(org.springframework.http.HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(expectedToken, response.getBody().token());
  }
}
