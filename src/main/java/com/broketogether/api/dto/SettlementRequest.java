package com.broketogether.api.dto;

import java.math.BigDecimal;

public class SettlementRequest {
  private Long homeId;
  private Long payeeId;
  private BigDecimal amount;

  public SettlementRequest() {

  }

  /**
   * @param homeId
   * @param payeeId
   * @param amount
   */
  public SettlementRequest(Long homeId, Long payeeId, BigDecimal amount) {
    this.homeId = homeId;
    this.payeeId = payeeId;
    this.amount = amount;
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
   * @return the payeeId
   */
  public Long getPayeeId() {
    return payeeId;
  }

  /**
   * @param payeeId the payeeId to set
   */
  public void setPayeeId(Long payeeId) {
    this.payeeId = payeeId;
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
