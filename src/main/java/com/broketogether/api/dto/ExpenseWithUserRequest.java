package com.broketogether.api.dto;

import java.math.BigDecimal;
import java.util.Set;

public class ExpenseWithUserRequest {

  private BigDecimal amount;

  private String description;

  private String category;

  private Long homeId;

  private Set<Long> userId;

  /**
   * 
   */
  public ExpenseWithUserRequest() {

  }

  /**
   * @param amount
   * @param description
   * @param category
   * @param homeId
   * @param userId
   */
  public ExpenseWithUserRequest(BigDecimal amount, String description, String category, Long homeId,
      Set<Long> userId) {
    this.amount = amount;
    this.description = description;
    this.category = category;
    this.homeId = homeId;
    this.userId = userId;
  }

  /**
   * @return the amount
   */
  public BigDecimal getAmount() {
    return amount;
  }

  /**
   * @param amount the amount to set
   */
  public void setAmount(BigDecimal amount) {
    this.amount = amount;
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
   * @return the homeId
   */
  public Long getHomeId() {
    return homeId;
  }

  /**
   * @param homeId the homeId to set
   */
  public void setHomeId(Long homeId) {
    this.homeId = homeId;
  }

  /**
   * @return the userId
   */
  public Set<Long> getUserId() {
    return userId;
  }

  /**
   * @param userId the userId to set
   */
  public void setUserId(Set<Long> userId) {
    this.userId = userId;
  }

}
