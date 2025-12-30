package com.broketogether.api.dto;

/**
 * Data Transfer Object for JWT authentication responses.
 * 
 * <p>
 * This is sent to the client after successful login, containing:
 * <ul>
 * <li>JWT token for subsequent authenticated requests</li>
 * <li>Token type (always "Bearer" for JWT)</li>
 * <li>User information (email and name)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Example JSON response:
 * 
 * <pre>
 * {
 *   "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhYmR1bGxhaEBleGFtcGxlLmNvbSIsImlhdCI6MTcwMzE4ODgwMCwiZXhwIjoxNzAzMjc1MjAwfQ.signature",
 *   "type": "Bearer",
 *   "email": "abdullah@example.com",
 *   "name": "Abdullah"
 * }
 * </pre>
 * </p>
 * 
 * <p>
 * The client should store the token and include it in the Authorization header
 * for protected endpoints:
 * 
 * <pre>
 * Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
 * </pre>
 * </p>
 */
public class JwtResponse {
  private String token;
  private String type = "Bearer";
  private String username;
  private String name;

  /**
   * Constructor for creating a JWT response.
   * 
   * @param token The JWT token string
   * @param email The user's email address
   * @param name  The user's display name
   */
  public JwtResponse(String token, String email, String name) {
    this.token = token;
    this.username = email;
    this.name = name;
  }

  /**
   * @return the token
   */
  public String getToken() {
    return token;
  }

  /**
   * @param token the token to set
   */
  public void setToken(String token) {
    this.token = token;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
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
  public void setName(String name) {
    this.name = name;
  }

}
