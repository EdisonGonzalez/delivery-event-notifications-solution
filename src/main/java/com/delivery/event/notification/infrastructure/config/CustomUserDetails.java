package com.delivery.event.notification.infrastructure.config;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Custom UserDetails implementation that includes clientId for BOLA protection. */
public class CustomUserDetails implements UserDetails {

  private final String username;
  private final String password;
  @Getter private final String clientId;
  private final List<GrantedAuthority> authorities;

  public CustomUserDetails(String username, String password, String clientId) {
    this(username, password, clientId, List.of("ROLE_CLIENT"));
  }

  public CustomUserDetails(
      String username, String password, String clientId, Collection<String> authorityNames) {
    this.username = username;
    this.password = password;
    this.clientId = clientId;
    this.authorities =
        authorityNames == null || authorityNames.isEmpty()
            ? List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
            : authorityNames.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
