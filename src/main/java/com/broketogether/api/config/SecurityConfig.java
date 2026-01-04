package com.broketogether.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
  private final JwtAuthenticationFilter jwtAuthFilter;

  /**
   * Constructor injection of required dependencies.
   * 
   * @param userDetailsService Service for loading user details from database
   * @param passwordEncoder    BCrypt encoder for password hashing/verification
   * @param jwtAuthFilter      Filter that validates JWT tokens on each request
   */
  public SecurityConfig(PasswordEncoder passwordEncoder, UserDetailsService userDetailsService,
      JwtAuthenticationFilter jwtAuthFilter) {

    this.passwordEncoder = passwordEncoder;
    this.userDetailsService = userDetailsService;
    this.jwtAuthFilter = jwtAuthFilter;
  }

  /**
   * Configures the authentication provider for Spring Security.
   * 
   * <p>
   * DaoAuthenticationProvider is responsible for:
   * <ul>
   * <li>Loading user details from database via CustomUserDetailsService</li>
   * <li>Verifying passwords using PasswordEncoder (BCrypt)</li>
   * <li>Creating Authentication objects for valid credentials</li>
   * </ul>
   * </p>
   * 
   * <p>
   * <b>Used by AuthenticationManager when user logs in:</b>
   * </p>
   * 
   * <pre>
   * User submits email + password
   *    ↓
   * AuthenticationManager.authenticate()
   *    ↓
   * DaoAuthenticationProvider
   *    ↓
   * CustomUserDetailsService.loadUserByUsername(email)
   *    ↓
   * UserRepository.findByEmail(email)
   *    ↓
   * Returns User entity
   *    ↓
   * PasswordEncoder.matches(rawPassword, hashedPassword)
   *    ↓
   * If match → Return Authentication object
   * If no match → Throw BadCredentialsException
   * </pre>
   * 
   * @return Configured DaoAuthenticationProvider
   */
  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder);

    return authProvider;
  }

  /**
   * Configures the security filter chain - the main security policy.
   * 
   * <p>
   * This method defines:
   * <ul>
   * <li>Which endpoints are public vs protected</li>
   * <li>How authentication is handled (stateless JWT)</li>
   * <li>Which filters are applied and in what order</li>
   * </ul>
   * </p>
   * 
   * <p>
   * <b>Filter Order:</b>
   * </p>
   * 
   * <pre>
   * Request
   *    ↓
   * JwtAuthenticationFilter (our custom filter - validates JWT)
   *    ↓
   * UsernamePasswordAuthenticationFilter (Spring's default - we skip this)
   *    ↓
   * Other Spring Security filters...
   *    ↓
   * Controller
   * </pre>
   * 
   * <p>
   * <b>Public Endpoints (no authentication required):</b>
   * </p>
   * <ul>
   * <li>POST /api/v1/auth/register - User registration</li>
   * <li>POST /api/v1/auth/login - User login (get JWT token)</li>
   * </ul>
   * 
   * <p>
   * <b>Protected Endpoints (JWT token required):</b>
   * </p>
   * <ul>
   * <li>All other endpoints - GET /api/v1/users, etc.</li>
   * </ul>
   * 
   * @param http HttpSecurity object for configuring security
   * @return Configured SecurityFilterChain
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // Public Endpoints
            .requestMatchers("/api/v1/auth/**", "/v3/api-docs/**", "/swagger-ui/**",
                "/swagger-ui.html")
            .permitAll()

            // Explicitly protect the Homes API (ensures matchers catch sub-paths)
            .requestMatchers("/api/v1/homes/**").authenticated()

            // All other requests
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Exposes AuthenticationManager as a bean.
   * 
   * <p>
   * AuthenticationManager is the main interface for authentication in Spring
   * Security. It's used by AuthController to validate user credentials during
   * login.
   * </p>
   * 
   * <p>
   * <b>Usage in AuthController:</b>
   * </p>
   * 
   * <pre>
   * Authentication auth = authenticationManager
   *     .authenticate(new UsernamePasswordAuthenticationToken(email, password));
   * </pre>
   * 
   * @param config Spring's authentication configuration
   * @return AuthenticationManager instance
   * @throws Exception if manager cannot be created
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

}
