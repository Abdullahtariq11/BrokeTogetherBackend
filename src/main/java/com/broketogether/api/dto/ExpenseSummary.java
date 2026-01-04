package com.broketogether.api.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for expense summary/statistics for a home.
 */
public class ExpenseSummary {

    private Long homeId;
    private String homeName;
    private BigDecimal totalExpenses;
    private Integer expenseCount;
    private Map<String, BigDecimal> expensesByCategory;  // Category -> Total amount
    private Map<Long, BigDecimal> expensesByUser;        // UserId -> Total paid

    // Constructors
    public ExpenseSummary() {
    }

    public ExpenseSummary(Long homeId, String homeName, BigDecimal totalExpenses,
                         Integer expenseCount) {
        this.homeId = homeId;
        this.homeName = homeName;
        this.totalExpenses = totalExpenses;
        this.expenseCount = expenseCount;
    }

    // Getters and Setters
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

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public Integer getExpenseCount() {
        return expenseCount;
    }

    public void setExpenseCount(Integer expenseCount) {
        this.expenseCount = expenseCount;
    }

    public Map<String, BigDecimal> getExpensesByCategory() {
        return expensesByCategory;
    }

    public void setExpensesByCategory(Map<String, BigDecimal> expensesByCategory) {
        this.expensesByCategory = expensesByCategory;
    }

    public Map<Long, BigDecimal> getExpensesByUser() {
        return expensesByUser;
    }

    public void setExpensesByUser(Map<Long, BigDecimal> expensesByUser) {
        this.expensesByUser = expensesByUser;
    }
}