package com.broketogether.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Main security configuration for the application. Defines the "Security Filter
 * Chain" which acts as a wall between incoming HTTP requests and your
 * Controllers.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final PasswordEncoder passwordEncoder;
  private final UserDetailsService userDetailsService;

  public SecurityConfig(PasswordEncoder passwordEncoder, UserDetailsService userDetailsService) {

    this.passwordEncoder = passwordEncoder;
    this.userDetailsService = userDetailsService;
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder);

    return authProvider;
  }

  /**
   * Configures the security policy for all HTTP requests.
   * 
   * @param http The HttpSecurity object used to build the filter chain.
   * @return The configured SecurityFilterChain bean.
   * @throws Exception if an error occurs during configuration.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    /*
     * 1. Disable CSRF (Cross-Site Request Forgery). We disable this for APIs
     * because we are using stateless Auth (or Postman), otherwise Spring blocks all
     * POST/PUT requests by default.
     */
    http.csrf(crsf -> crsf.disable())
        /*
         * 2. URL Authorization Rules. This section defines which endpoints are public
         * and which are private.
         */
        // For now, we require authentication for ALL requests.
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/users")
                .permitAll().anyRequest().authenticated())
        /*
         * 3. Enable Basic Authentication. This allows us to use the 'user' and
         * 'password' in Postman's Authorization tab (Basic Auth).
         */
        .httpBasic(Customizer.withDefaults());

    return http.build();
  }

}
