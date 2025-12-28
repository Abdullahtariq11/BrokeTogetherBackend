package com.broketogether.api.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * User entity class , holds infroamtion for user.
 */
@Entity
@Table(name = "users") // map to users table
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "full_name", nullable = false, length = 100)
  private String fullname;

  @Column(unique = true, nullable = false, length = 100)
  private String email;

  @Column(nullable = false, length = 100)
  private String password;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false, length = 100)
  private String role;

  /**
   * JPA requires no args constructor
   */
  public User() {

  }


  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * @return the fullname
   */
  public String getFullname() {
    return fullname;
  }

  /**
   * @param fullname the fullname to set
   */
  public void setFullname(String fullname) {
    this.fullname = fullname;
  }

  /**
   * @return the username
   */
  public String getEmail() {
    return email;
  }

  /**
   * @return the role of the user
   */
  public String getRole() {
    return role;
  }

  /**
   * @param sets the role for user
   */
  public void setRole(String role) {
    this.role = role;
  }

  /**
   * @param username the username to set
   */
  public void setEmail(String email) {
    this.email = email;
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

  /**
   * @return createdAt date for the user
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  @Override
  public String toString() {
    return "User [id=" + id + ", fullname=" + fullname + ", email=" + email + ", password="
        + password + ", createdAt=" + createdAt + ", role=" + role + "]";
  }

}
