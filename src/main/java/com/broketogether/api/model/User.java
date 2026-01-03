package com.broketogether.api.model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * User entity class , holds infroamtion for user.
 */
@Entity
@Table(name = "users") // map to users table
public class User implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "full_name", nullable = false, length = 100)
  private String name;

  @Column(unique = true, nullable = false, length = 100)
  private String email;

  @JsonIgnore
  @Column(nullable = false, length = 100)
  private String password;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false, length = 100)
  private String role;

  @ManyToMany(mappedBy = "members")
  @JsonIgnore // Crucial: prevents infinite loops in JSON responses
  private Set<Home> homes = new HashSet<>();

  //Homes where this user is the "Admin"
  @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
  @JsonIgnore
  private Set<Home> homesOwned = new HashSet<>();

  /**
   * JPA requires no args constructor
   */
  public User() {

  }

  /**
   * 
   */
  public User(String name, String username, String password) {
    this.name = name;
    this.email = username;
    this.password = password;
    this.role = "USER";
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
  public String getName() {
    return name;
  }

  /**
   * @param name the fullname to set
   */
  public void setName(String name) {
    this.name = name;
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
   * @param role sets the role for user
   */
  public void setRole(String role) {
    this.role = role;
  }

  /**
   * @param email the username to set
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
  

  /**
   * @return the homes
   */
  public Set<Home> getHomes() {
    return homes;
  }

  /**
   * @param homes the homes to set
   */
  public void setHomes(Set<Home> homes) {
    this.homes = homes;
  }

  /**
   * @return the homesOwned
   */
  public Set<Home> getHomesOwned() {
    return homesOwned;
  }

  /**
   * @param homesOwned the homesOwned to set
   */
  public void setHomesOwned(Set<Home> homesOwned) {
    this.homesOwned = homesOwned;
  }

  @Override
  public String toString() {
    return "User [id=" + id + ", name=" + name + ", email=" + email + ", password=" + password
        + ", createdAt=" + createdAt + ", role=" + role + "]";
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {

    return List.of(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Override
  public String getUsername() {

    return this.email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

}
