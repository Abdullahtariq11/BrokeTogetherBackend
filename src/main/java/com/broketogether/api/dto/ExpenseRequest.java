package com.broketogether.api.dto;

import java.math.BigDecimal;

/**
 * DTO for creating a new expense.
 */
public class ExpenseRequest {


  private BigDecimal amount;

  private String description;

  private String category;

  private Long homeId;

  /**
   * Default Constructor
   */
  public ExpenseRequest() {

  }

  /**
   * @param id
   * @param amount
   * @param description
   * @param category
   */
  public ExpenseRequest(BigDecimal amount, String description, String category, Long homeId) {
    this.amount = amount;
    this.description = description;
    this.category = category;
    this.homeId = homeId;
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

}