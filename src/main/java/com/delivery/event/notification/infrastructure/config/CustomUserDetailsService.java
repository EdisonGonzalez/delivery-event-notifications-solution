package com.delivery.event.notification.infrastructure.config;

import com.delivery.event.notification.infrastructure.persistence.jpa.UserJpaRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Custom UserDetailsService that loads users from the database. */
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserJpaRepository userRepository;

  /**
   * @param userRepository JPA repository for users.
   */
  public CustomUserDetailsService(UserJpaRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Load user details by username from the database.
   *
   * @param username the username
   * @return UserDetails with credentials, authorities, and clientId for BOLA protection
   * @throws UsernameNotFoundException if the user is not found or disabled
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userRepository
        .findByUsernameAndEnabled(username)
        .map(
            user ->
                new CustomUserDetails(
                    user.getUsername(),
                    user.getPassword(),
                    user.getClientId(),
                    parseAuthorities(user.getRoles())))
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }

  private List<String> parseAuthorities(Collection<String> roles) {
    if (roles == null || roles.isEmpty()) {
      return List.of("ROLE_CLIENT");
    }

    return roles.stream()
        .map(String::trim)
        .filter(role -> !role.isBlank())
        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
        .distinct()
        .toList();
  }
}


