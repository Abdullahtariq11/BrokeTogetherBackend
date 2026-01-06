package com.broketogether.api.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for expense responses. Contains expense details and how it's split.
 */
public class ExpenseResponse {

  private Long id;

  private Double amount;

  private String Description;

  private String Category;

  // <user_id,expens_id
  Map<Long, ExpenseSplitResponse> splits = new HashMap<>();

  public ExpenseResponse() {

  }

  /**
   * @param id
   * @param amount
   * @param description
   * @param category
   * @param splits
   */
  public ExpenseResponse(Long id, Double amount, String description, String category,
      Map<Long, ExpenseSplitResponse> splits) {
    this.id = id;
    this.amount = amount;
    Description = description;
    Category = category;
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
   * @return the description
   */
  public String getDescription() {
    return Description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description) {
    Description = description;
  }

  /**
   * @return the category
   */
  public String getCategory() {
    return Category;
  }

  /**
   * @param category the category to set
   */
  public void setCategory(String category) {
    Category = category;
  }

  /**
   * @return the splits
   */
  public Map<Long, ExpenseSplitResponse> getSplits() {
    return splits;
  }

  /**
   * @param splits the splits to set
   */
  public void setSplits(Map<Long, ExpenseSplitResponse> splits) {
    this.splits = splits;
  }

}