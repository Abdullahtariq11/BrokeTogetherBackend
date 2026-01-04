package com.broketogether.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for creating a new expense.
 */
public class ExpenseRequest {

    private String description;
    private BigDecimal amount;
    private String category;
    private Long homeId;
    private String splitType;  // "EQUAL", "CUSTOM", "PERCENTAGE"
    private LocalDateTime expenseDate;
    
    /**
     * Custom splits (only used when splitType = "CUSTOM" or "PERCENTAGE")
     * Map of userId -> amount (for CUSTOM) or userId -> percentage (for PERCENTAGE)
     * 
     * Example for CUSTOM:
     * {
     *   "1": 30.00,
     *   "2": 20.00,
     *   "3": 50.00
     * }
     * 
     * Example for PERCENTAGE:
     * {
     *   "1": 30,  // 30%
     *   "2": 20,  // 20%
     *   "3": 50   // 50%
     * }
     */
    private Map<Long, BigDecimal> customSplits;

    // Constructors
    public ExpenseRequest() {
    }

    public ExpenseRequest(String description, BigDecimal amount, String category, 
                         Long homeId, String splitType) {
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.homeId = homeId;
        this.splitType = splitType;
        this.expenseDate = LocalDateTime.now();
    }

    // Getters and Setters
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

    public Long getHomeId() {
        return homeId;
    }

    public void setHomeId(Long homeId) {
        this.homeId = homeId;
    }

    public String getSplitType() {
        return splitType;
    }

    public void setSplitType(String splitType) {
        this.splitType = splitType;
    }

    public LocalDateTime getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDateTime expenseDate) {
        this.expenseDate = expenseDate;
    }

    public Map<Long, BigDecimal> getCustomSplits() {
        return customSplits;
    }

    public void setCustomSplits(Map<Long, BigDecimal> customSplits) {
        this.customSplits = customSplits;
    }
}