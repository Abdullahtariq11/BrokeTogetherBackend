package com.broketogether.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.broketogether.api.model.User;

/**
 * Repository interface for User entity. Provides standard CRUD operations and
 * custom query methods.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * @param email is used to find user account
   * @return a user or a null value
   */
  Optional<User> findByEmail(String email);

  /**
   * @param email is used to check if user exist
   * @return true/false depending on if user exists
   */
  boolean existsByEmail(String email);
}
