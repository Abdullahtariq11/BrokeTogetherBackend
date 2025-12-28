package com.broketogether.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration class for password encryption. This class defines the bean used
 * to hash and verify passwords across the application.
 */
@Configuration
public class PasswordConfig {

  /**
   * Configures the PasswordEncoder bean to use the BCrypt hashing algorithm.
   * BCrypt is a "salted" hashing function, which makes it resistant to rainbow
   * table attacks and provides strong security for user credentials.
   * 
   * @return A BCryptPasswordEncoder instance used for password hashing.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    // Default strength is 10, which is currently the industry standard
    // balance between security and performance.
    return new BCryptPasswordEncoder();
  }
}
