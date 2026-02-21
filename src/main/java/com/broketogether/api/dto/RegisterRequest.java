package com.broketogether.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user registration requests.
 */
public class RegisterRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Please provide a valid email address")
  private String username;

  @NotBlank(message = "Name is required")
  @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
  private String name;

  @NotBlank(message = "Password is required")
  @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
  private String password;

  public RegisterRequest() {
  }

  public RegisterRequest(String name, String email, String password) {
    this.name = name;
    this.username = email;
    this.password = password;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

}
