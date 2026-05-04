package com.broketogether.api.controller;

import javax.security.auth.login.AccountNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.broketogether.api.model.User;
import com.broketogether.api.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

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

  /**
   * Permanently deletes the current user's account, removes them from all homes,
   * and deletes any homes they own along with associated expenses.
   *
   * @return 204 No Content on success
   */
  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteAccount() throws AccountNotFoundException {
    userService.deleteAccount();
    return ResponseEntity.noContent().build();
  }

}
