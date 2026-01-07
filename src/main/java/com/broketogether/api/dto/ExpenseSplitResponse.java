package com.broketogether.api.dto;

import java.math.BigDecimal;

public class ExpenseSplitResponse {
  private Long id;

  private BigDecimal amount;

  public ExpenseSplitResponse() {

  }

  /**
   * @param id
   * @param amount
   */
  public ExpenseSplitResponse(Long id, BigDecimal amount) {
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
  public BigDecimal getAmount() {
    return amount;
  }

  /**
   * @param amount the amount to set
   */
  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

}
