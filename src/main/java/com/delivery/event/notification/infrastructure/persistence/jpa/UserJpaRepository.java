package com.delivery.event.notification.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for users. */
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

  /**
   * Find a user by username.
   *
   * @param username the username
   * @return the user, if found
   */
  @Query("SELECT u FROM UserEntity u WHERE u.username = :username AND u.enabled = true")
  Optional<UserEntity> findByUsernameAndEnabled(@Param("username") String username);
}

