package com.broketogether.api.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "homes")
public class Home {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false)
  private String name;

  @Column(unique = true, nullable = false)
  private String inviteCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator_id", nullable = false)
  @JsonIgnore
  private User creator;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "user_homes", joinColumns = @JoinColumn(name = "home_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
  @JsonIgnore
  private Set<User> members = new HashSet<>();

  /**
   * Default Constructor
   */
  public Home() {
  }

  /**
   * Constructor with parameters
   * 
   * @param name
   * @param creator
   * @param members
   */
  public Home(String name, User creator, Set<User> members) {
    this.name = name;
    this.creator = creator;
    this.members = members;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the id
   */
  public long getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * @return the inviteCode
   */
  public String getInviteCode() {
    return inviteCode;
  }

  /**
   * @param inviteCode the inviteCode to set
   */
  public void setInviteCode(String inviteCode) {
    this.inviteCode = inviteCode;
  }

  /**
   * @return the creator
   */
  public User getCreator() {
    return creator;
  }

  /**
   * @param creator the creator to set
   */
  public void setCreator(User creator) {
    this.creator = creator;
  }

  /**
   * @return the members
   */
  public Set<User> getMembers() {
    return members;
  }

  /**
   * @param members the members to set
   */
  public void setMembers(Set<User> members) {
    this.members = members;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Automatically generates a unique 8-character invite code before the record is
   * saved to the database.
   */
  @PrePersist
  public void generateInviteCode() {
    if (this.inviteCode == null) {
      this.inviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
  }

}
