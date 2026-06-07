package com.delivery.event.notification.infrastructure.web;

import com.delivery.event.notification.infrastructure.config.CustomUserDetails;
import com.delivery.event.notification.infrastructure.config.CustomUserDetailsService;
import com.delivery.event.notification.infrastructure.config.JwtUtil;
import com.delivery.event.notification.infrastructure.web.dto.auth.AuthLoginRequest;
import com.delivery.event.notification.infrastructure.web.dto.auth.AuthLoginResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authentication controller for JWT token generation. */
@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final CustomUserDetailsService userDetailsService;
  private final JwtUtil jwtUtil;

  public AuthController(
      AuthenticationManager authenticationManager,
      CustomUserDetailsService userDetailsService,
      JwtUtil jwtUtil) {
    this.authenticationManager = authenticationManager;
    this.userDetailsService = userDetailsService;
    this.jwtUtil = jwtUtil;
  }

  /**
   * Authenticate user and return JWT token.
   *
   * @param request Login request with username and password
   * @return JWT token
   */
  @PostMapping("/login")
  public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password()));

    UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
    String clientId = ((CustomUserDetails) userDetails).getClientId();
    List<String> authorities =
        userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    String token = jwtUtil.generateToken(userDetails.getUsername(), clientId, authorities);

    return ResponseEntity.ok(new AuthLoginResponse(token));
  }
}
