package com.broketogether.api.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Marks the last time a user settled up in a home. Expenses created before
 * this timestamp are shown as settled/closed in the activity feed, purely
 * as a UI cue — it has no effect on the underlying balance calculation.
 */
@Entity
@Table(name = "settlement_checkpoints",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "home_id"}))
public class SettlementCheckpoint {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "home_id", nullable = false)
  private Home home;

  @Column(name = "settled_at", nullable = false)
  private LocalDateTime settledAt;

  public SettlementCheckpoint() {
  }

  public SettlementCheckpoint(User user, Home home, LocalDateTime settledAt) {
    this.user = user;
    this.home = home;
    this.settledAt = settledAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Home getHome() {
    return home;
  }

  public void setHome(Home home) {
    this.home = home;
  }

  public LocalDateTime getSettledAt() {
    return settledAt;
  }

  public void setSettledAt(LocalDateTime settledAt) {
    this.settledAt = settledAt;
  }

}
