package com.broketogether.api.dto;

import com.broketogether.api.dto.enums.SplitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Request body for PATCH /expenses/{id}.
 * All fields are optional — only non-null values are applied.
 */
public class PatchExpenseRequest {

    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @Size(min = 1, max = 255)
    private String description;

    private String category;

    private SplitType splitType;

    /** Optional payer override — must be a household member. */
    private Long payerId;

    private Set<Long> userIds;

    private Map<Long, BigDecimal> exactSplits;

    private BigDecimal payerFixedAmount;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public SplitType getSplitType() { return splitType; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }

    public Long getPayerId() { return payerId; }
    public void setPayerId(Long payerId) { this.payerId = payerId; }

    public Set<Long> getUserIds() { return userIds; }
    public void setUserIds(Set<Long> userIds) { this.userIds = userIds; }

    public Map<Long, BigDecimal> getExactSplits() { return exactSplits; }
    public void setExactSplits(Map<Long, BigDecimal> exactSplits) { this.exactSplits = exactSplits; }

    public BigDecimal getPayerFixedAmount() { return payerFixedAmount; }
    public void setPayerFixedAmount(BigDecimal payerFixedAmount) { this.payerFixedAmount = payerFixedAmount; }
}
