package com.broketogether.api.controller;

import javax.security.auth.login.AccountNotFoundException;

import com.broketogether.api.dto.EditNameRequest;
import com.broketogether.api.dto.PasswordResetRequest;
import com.broketogether.api.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
   * @return UserResponse with the current user's profile
   */
  @GetMapping("/me")
  public ResponseEntity<UserResponse> getCurrentUser() throws AccountNotFoundException {
    // Always fetch fresh from DB so fields like isPremium reflect the latest state
    return ResponseEntity.ok(userService.getProfile());
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

  /**
   * Reset Password
   *
   */
  @PostMapping("/password-reset")
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) throws AccountNotFoundException {
    userService.resetPassword(request.getCurrentPassword(), request.getNewPassword());
    return ResponseEntity.noContent().build();
  }

  /**
   * Edit user name
   *
   */
  @PutMapping("/edit")
  public ResponseEntity<UserResponse> editName(@Valid @RequestBody EditNameRequest request) throws AccountNotFoundException {
    return ResponseEntity.ok(userService.editUserName(request.getName()));
  }


}
