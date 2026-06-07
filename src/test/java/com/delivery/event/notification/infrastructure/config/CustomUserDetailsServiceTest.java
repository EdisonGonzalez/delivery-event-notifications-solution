package com.delivery.event.notification.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.delivery.event.notification.infrastructure.persistence.jpa.UserEntity;
import com.delivery.event.notification.infrastructure.persistence.jpa.UserJpaRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

  @Mock private UserJpaRepository userRepository;

  @Test
  void shouldNormalizeRolesLoadedFromDatabase() {
    UserEntity user = new UserEntity();
    user.setId(UUID.randomUUID());
    user.setUsername("client-a");
    user.setPassword("encoded-password");
    user.setClientId("client-a");
    user.setEnabled(true);
    user.setRoles(Set.of("CLIENT", "ROLE_ADMIN"));

    when(userRepository.findByUsernameAndEnabled("client-a")).thenReturn(Optional.of(user));

    CustomUserDetailsService service = new CustomUserDetailsService(userRepository);
    CustomUserDetails userDetails = (CustomUserDetails) service.loadUserByUsername("client-a");

    assertTrue(
        userDetails.getAuthorities().stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_CLIENT")));
    assertTrue(
        userDetails.getAuthorities().stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN")));
  }
}

