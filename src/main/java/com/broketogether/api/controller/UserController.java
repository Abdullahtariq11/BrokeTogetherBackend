package com.broketogether.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.broketogether.api.model.User;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  /**
   * Get the current authenticated user's profile.
   *
   * @param currentUser The authenticated user (automatically injected by Spring)
   * @return ResponseEntity with the current user's profile
   */
  @GetMapping("/me")
  public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal User currentUser) {
    return ResponseEntity.ok(currentUser);
  }

}
