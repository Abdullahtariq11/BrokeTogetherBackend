package com.broketogether.api.dto;

/**
 * Data Transfer Object for user registration requests.
 * 
 * <p>
 * Captures the information needed to create a new user account. This separates
 * the registration API contract from the User entity, allowing the API to
 * evolve independently of the database schema.
 * </p>
 * 
 * <p>
 * Example JSON request body:
 * 
 * <pre>
 * {
 *   "email": "abdullah@example.com",
 *   "password": "mySecurePassword123",
 *   "name": "Abdullah"
 * }
 * </pre>
 * </p>
 * 
 * <p>
 * <b>Note:</b> The password will be BCrypt-hashed by the UserService before
 * being stored in the database. Never store plain-text passwords.
 * </p>
 */
public class RegisterRequest {

  private String username;
  private String name;
  private String password;

  // Default constructor (required for JSON deserialization)
  public RegisterRequest() {
  }

  /**
   * Constructor for creating a registration request.
   * 
   * @param email    The user's email address
   * @param password The user's password (will be hashed)
   * @param name     The user's display name
   */
  public RegisterRequest(String name,String email, String password) {
    this.name = name;
    this.username = email;
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
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void name(String name) {
    this.name = name;
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
