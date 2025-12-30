package com.broketogether.api.dto;

/**
 * Data Transfer Object for user login requests.
 */
public class LoginRequest {
  private String username;
  private String password;

  /**
   * Default constructor required for json serialization.
   */
  public LoginRequest() {

  }

  /**
   * Constructor for creating a login request with email and password.
   * 
   * @param username The user's email address
   * @param password The user's password
   */
  public LoginRequest(String username, String password) {
    this.username = username;
    this.password = password;
  }

  /**
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * @param username the username to set
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param password the password to set
   */
  public void setPassword(String password) {
    this.password = password;
  }

}
