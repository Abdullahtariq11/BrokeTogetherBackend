package com.broketogether.api.dto;

public class ExpenseSplitResponse {
  private Long id;

  private double amount;

  public ExpenseSplitResponse() {

  }

  /**
   * @param id
   * @param amount
   */
  public ExpenseSplitResponse(Long id, double amount) {
    this.id = id;
    this.amount = amount;
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
  public double getAmount() {
    return amount;
  }

  /**
   * @param amount the amount to set
   */
  public void setAmount(double amount) {
    this.amount = amount;
  }

}
