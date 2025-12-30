package com.broketogether.api.controller;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.broketogether.api.config.JwtUtils;
import com.broketogether.api.dto.JwtResponse;
import com.broketogether.api.dto.LoginRequest;
import com.broketogether.api.dto.RegisterRequest;
import com.broketogether.api.model.User;
import com.broketogether.api.service.UserService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final UserService userService;
  private final AuthenticationManager authenticationManager;
  private final JwtUtils jwtUtils;

  public AuthController(UserService userService, AuthenticationManager authenticationManager,
      JwtUtils jwtUtils) {
    this.userService = userService;
    this.authenticationManager = authenticationManager;
    this.jwtUtils = jwtUtils;
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
  public ResponseEntity<?> Login(@RequestBody LoginRequest loginRequest) {

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

    JwtResponse response = new JwtResponse(jwt, userDetails.getEmail(), userDetails.getFullname());

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
  public ResponseEntity<User> createUser(@RequestBody RegisterRequest registerRequest)
      throws NotFoundException {

    User user = new User(registerRequest.getName(), registerRequest.getUsername(),
        registerRequest.getPassword());

    User savedUser = userService.saveUser(user);

    return ResponseEntity.status(201).body(savedUser);
  }

}
