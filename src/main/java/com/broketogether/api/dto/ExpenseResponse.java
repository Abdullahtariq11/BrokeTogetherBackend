package com.broketogether.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for expense responses.
 * Contains expense details and how it's split.
 */
public class ExpenseResponse {

    private Long id;
    private String description;
    private BigDecimal amount;
    private String category;
    private Long payerId;
    private String payerName;
    private Long homeId;
    private String homeName;
    private List<SplitInfo> splits;
    private LocalDateTime expenseDate;
    private LocalDateTime createdAt;

    // Nested class for split information
    public static class SplitInfo {
        private Long userId;
        private String userName;
        private BigDecimal amount;

        public SplitInfo() {
        }

        public SplitInfo(Long userId, String userName, BigDecimal amount) {
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
    public ExpenseResponse() {
    }

    public ExpenseResponse(Long id, String description, BigDecimal amount, String category,
                          Long payerId, String payerName, Long homeId, String homeName,
                          List<SplitInfo> splits, LocalDateTime expenseDate, LocalDateTime createdAt) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.payerId = payerId;
        this.payerName = payerName;
        this.homeId = homeId;
        this.homeName = homeName;
        this.splits = splits;
        this.expenseDate = expenseDate;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getPayerId() {
        return payerId;
    }

    public void setPayerId(Long payerId) {
        this.payerId = payerId;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public Long getHomeId() {
        return homeId;
    }

    public void setHomeId(Long homeId) {
        this.homeId = homeId;
    }

    public String getHomeName() {
        return homeName;
    }

    public void setHomeName(String homeName) {
        this.homeName = homeName;
    }

    public List<SplitInfo> getSplits() {
        return splits;
    }

    public void setSplits(List<SplitInfo> splits) {
        this.splits = splits;
    }

    public LocalDateTime getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDateTime expenseDate) {
        this.expenseDate = expenseDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}