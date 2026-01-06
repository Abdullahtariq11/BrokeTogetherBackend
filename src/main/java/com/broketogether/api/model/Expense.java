package com.broketogether.api.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
public class Expense {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String description;

  @Column(nullable = false, precision = 10, scale = 2)
  private Double amount;

  @Column(length = 50)
  private String category;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payer_id", nullable = false)
  @JsonIgnore
  private User payer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "home_id", nullable = false)
  @JsonIgnore
  private Home home;

  @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ExpenseSplit> splits = new ArrayList<>();

  public Expense() {

  }

  public Expense(Long id, String description, Double amount, String category,
      LocalDateTime createdAt, User payer, Home home, List<ExpenseSplit> splits) {
    this.id = id;
    this.description = description;
    this.amount = amount;
    this.category = category;
    this.createdAt = createdAt;
    this.payer = payer;
    this.home = home;
    this.splits = splits;
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
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the amount
   */
  public Double getAmount() {
    return amount;
  }

  /**
   * @param amount the amount to set
   */
  public void setAmount(Double amount) {
    this.amount = amount;
  }

  /**
   * @return the category
   */
  public String getCategory() {
    return category;
  }

  /**
   * @param category the category to set
   */
  public void setCategory(String category) {
    this.category = category;
  }

  /**
   * @return the createdAt
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * @param createdAt the createdAt to set
   */
  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * @return the payer
   */
  public User getPayer() {
    return payer;
  }

  /**
   * @param payer the payer to set
   */
  public void setPayer(User payer) {
    this.payer = payer;
  }

  /**
   * @return the home
   */
  public Home getHome() {
    return home;
  }

  /**
   * @param home the home to set
   */
  public void setHome(Home home) {
    this.home = home;
  }

  /**
   * @return the splits
   */
  public List<ExpenseSplit> getSplits() {
    return splits;
  }

  /**
   * @param splits the splits to set
   */
  public void setSplits(List<ExpenseSplit> splits) {
    this.splits = splits;
  }

}
