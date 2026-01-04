package com.broketogether.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for user balance information in a home.
 */
public class BalanceResponse {

  private Long userId;
  private String userName;
  private BigDecimal totalPaid; // Total amount user has paid
  private BigDecimal totalOwed; // Total amount user owes
  private BigDecimal balance; // Net balance (paid - owed)
  private List<DebtInfo> debts; // Who owes this user money
  private List<DebtInfo> owes; // Who this user owes money to

  // Nested class for debt information
  public static class DebtInfo {
    private Long userId;
    private String userName;
    private BigDecimal amount;

    public DebtInfo() {
    }

    public DebtInfo(Long userId, String userName, BigDecimal amount) {
      this.userId = userId;
      this.userName = userName;
      this.amount = amount;
    }

    // Getters and setters
    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long userId) {
      this.userId = userId;
    }

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal amount) {
      this.amount = amount;
    }
  }

  // Constructors
  public BalanceResponse() {
  }

  public BalanceResponse(Long userId, String userName, BigDecimal totalPaid, BigDecimal totalOwed,
      BigDecimal balance) {
    this.userId = userId;
    this.userName = userName;
    this.totalPaid = totalPaid;
    this.totalOwed = totalOwed;
    this.balance = balance;
  }

  // Getters and Setters
  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public BigDecimal getTotalPaid() {
    return totalPaid;
  }

  public void setTotalPaid(BigDecimal totalPaid) {
    this.totalPaid = totalPaid;
  }

  public BigDecimal getTotalOwed() {
    return totalOwed;
  }

  public void setTotalOwed(BigDecimal totalOwed) {
    this.totalOwed = totalOwed;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  public List<DebtInfo> getDebts() {
    return debts;
  }

  public void setDebts(List<DebtInfo> debts) {
    this.debts = debts;
  }

  public List<DebtInfo> getOwes() {
    return owes;
  }

  public void setOwes(List<DebtInfo> owes) {
    this.owes = owes;
  }
}