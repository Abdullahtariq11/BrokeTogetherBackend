package com.broketogether.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class SettlementRequest {

  @NotNull(message = "Home ID is required")
  private Long homeId;

  @NotNull(message = "Payee ID is required")
  private Long payeeId;

  @NotNull(message = "Amount is required")
  @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
  private BigDecimal amount;

  public SettlementRequest() {
  }

  public SettlementRequest(Long homeId, Long payeeId, BigDecimal amount) {
    this.homeId = homeId;
    this.payeeId = payeeId;
    this.amount = amount;
  }

  public Long getHomeId() {
    return homeId;
  }

  public void setHomeId(Long homeId) {
    this.homeId = homeId;
  }

  public Long getPayeeId() {
    return payeeId;
  }

  public void setPayeeId(Long payeeId) {
    this.payeeId = payeeId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

}
