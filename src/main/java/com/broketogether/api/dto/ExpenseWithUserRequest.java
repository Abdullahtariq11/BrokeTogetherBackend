package com.broketogether.api.dto;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ExpenseWithUserRequest {

  @NotNull(message = "Amount is required")
  @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
  private BigDecimal amount;

  @NotBlank(message = "Description is required")
  @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
  private String description;

  @NotBlank(message = "Category is required")
  private String category;

  @NotNull(message = "Home ID is required")
  private Long homeId;

  @NotEmpty(message = "At least one user must be selected for splitting")
  private Set<Long> userId;

  public ExpenseWithUserRequest() {
  }

  public ExpenseWithUserRequest(BigDecimal amount, String description, String category, Long homeId,
      Set<Long> userId) {
    this.amount = amount;
    this.description = description;
    this.category = category;
    this.homeId = homeId;
    this.userId = userId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public Long getHomeId() {
    return homeId;
  }

  public void setHomeId(Long homeId) {
    this.homeId = homeId;
  }

  public Set<Long> getUserId() {
    return userId;
  }

  public void setUserId(Set<Long> userId) {
    this.userId = userId;
  }

}
