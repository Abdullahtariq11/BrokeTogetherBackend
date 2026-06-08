package com.broketogether.api.controller;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.broketogether.api.config.JwtUtils;
import com.broketogether.api.dto.ForgotPasswordRequest;
import com.broketogether.api.dto.GoogleMobileRequest;
import com.broketogether.api.dto.JwtResponse;
import com.broketogether.api.dto.LoginRequest;
import com.broketogether.api.dto.RegisterRequest;
import com.broketogether.api.dto.TokenResetPasswordRequest;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.UserRepository;
import com.broketogether.api.service.PasswordResetService;
import com.broketogether.api.service.UserService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final UserService userService;
  private final AuthenticationManager authenticationManager;
  private final JwtUtils jwtUtils;
  private final PasswordResetService passwordResetService;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthController(UserService userService, AuthenticationManager authenticationManager,
      JwtUtils jwtUtils, PasswordResetService passwordResetService,
      UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userService = userService;
    this.authenticationManager = authenticationManager;
    this.jwtUtils = jwtUtils;
    this.passwordResetService = passwordResetService;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Login endpoint - Authenticates user and returns JWT token.
   *
   * @param loginRequest DTO containing user's email and password
   *
   * @return ResponseEntity with JwtResponse containing token and user info
   * @throws org.springframework.security.authentication.BadCredentialsException if
   *                                                                             credentials
   *                                                                             are
   *                                                                             invalid
   */
  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {

    // This is NOT the JWT token - it's Spring Security's authentication object
    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
        loginRequest.getUsername(), loginRequest.getPassword());

    // AuthenticationManager will:
    // - Call CustomUserDetailsService.loadUserByUsername(email)
    // - Load User from database
    // - Compare the provided password with the stored BCrypt hash
    // - If valid: return Authentication object with UserDetails
    // - If invalid: throw BadCredentialsException (Spring handles this → 401)
    Authentication authentication = authenticationManager.authenticate(authToken);

    // This makes the user "logged in" for this request
    // (Not really necessary for login endpoint, but good practice)
    SecurityContextHolder.getContext().setAuthentication(authentication);

    String jwt = jwtUtils.generateToken(authentication);
    User userDetails = (User) authentication.getPrincipal();

    JwtResponse response = new JwtResponse(jwt, userDetails.getEmail(), userDetails.getName());

    return ResponseEntity.ok(response);
  }

  /**
   * Registration endpoint - Creates a new user account. User must call the /login
   * endpoint to receive a JWT token.
   *
   * @param registerRequest DTO containing new user's email, password, and name
   * @return ResponseEntity with success message
   * @throws IllegalArgumentException if email already exists (handled by
   *                                  UserService)
   */
  @PostMapping("/register")
  public ResponseEntity<String> createUser(@Valid @RequestBody RegisterRequest registerRequest)
      throws NotFoundException {

    User user = new User(registerRequest.getName(), registerRequest.getUsername(),
        registerRequest.getPassword());

    userService.saveUser(user);

    return ResponseEntity.status(201).body("Account created successfully.");
  }

  /**
   * Sends a password reset email if the address is registered.
   * Always returns 200 to prevent email enumeration.
   */
  @PostMapping("/forgot-password")
  public ResponseEntity<String> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {
    passwordResetService.initiateReset(request.getEmail());
    return ResponseEntity.ok("If that email is registered, a reset link has been sent.");
  }

  /**
   * Resets the user's password using a valid reset token.
   */
  @PostMapping("/reset-password")
  public ResponseEntity<String> resetPassword(
      @Valid @RequestBody TokenResetPasswordRequest request) {
    passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
    return ResponseEntity.ok("Password reset successfully.");
  }

  /**
   * Mobile Google Sign-In — verifies a Google access token, finds or creates
   * the user, and returns a BrokeTogether JWT.
   *
   * Called by the React Native app after it completes the Google OAuth2 flow
   * via expo-auth-session. The mobile client sends the Google access token;
   * we call Google's userinfo endpoint to obtain the verified email + name.
   */
  @PostMapping("/google/mobile")
  public ResponseEntity<JwtResponse> googleMobileLogin(
      @Valid @RequestBody GoogleMobileRequest request) {

    // Verify the access token by calling Google's userinfo endpoint
    RestTemplate rest = new RestTemplate();
    @SuppressWarnings("unchecked")
    Map<String, Object> googleUser = rest.getForObject(
        "https://www.googleapis.com/oauth2/v3/userinfo",
        Map.class,
        Map.of()
    );

    // Attach the token in the Authorization header manually
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.setBearerAuth(request.getAccessToken());
    org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

    @SuppressWarnings("unchecked")
    Map<String, Object> profile = rest.exchange(
        "https://www.googleapis.com/oauth2/v3/userinfo",
        org.springframework.http.HttpMethod.GET,
        entity,
        Map.class
    ).getBody();

    if (profile == null || profile.get("email") == null) {
      throw new IllegalArgumentException("Invalid Google access token.");
    }

    String email = ((String) profile.get("email")).toLowerCase().trim();
    String name  = (String) profile.getOrDefault("name", email);

    // Find or create the user (mirrors OAuth2SuccessHandler logic)
    User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
      User newUser = new User(name, email,
          passwordEncoder.encode(UUID.randomUUID().toString()));
      return userRepository.save(newUser);
    });

    String jwt = jwtUtils.generateToken(user);
    return ResponseEntity.ok(new JwtResponse(jwt, user.getEmail(), user.getName()));
  }

}
