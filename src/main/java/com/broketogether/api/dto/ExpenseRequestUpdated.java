package com.broketogether.api.dto;

import com.broketogether.api.dto.enums.SplitType;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public class ExpenseRequestUpdated {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal totalAmount;

    @NotBlank(message = "Description is required")
    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Home ID is required")
    private Long homeId;


    private Set<Long> userIds;

    private Map<Long, BigDecimal> exactSplits;
    private BigDecimal payerFixedAmount;

    @NotNull
    private SplitType splitType = SplitType.EQUAL;

    public ExpenseRequestUpdated(BigDecimal totalAmount, String description, String category,
                                 Set<Long> userIds,Map<Long, BigDecimal> exactSplits,
                                 SplitType splitType, BigDecimal payerFixedAmount) {
        this.totalAmount = totalAmount;
        this.description = description;
        this.category = category;
        this.userIds = userIds;
        this.splitType = splitType;
        this.exactSplits = exactSplits;
        this.payerFixedAmount=payerFixedAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getHomeId() {
        return homeId;
    }

    public void setHomeId(Long homeId) {
        this.homeId = homeId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Set<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(Set<Long> userIds) {
        this.userIds = userIds;
    }

    public Map<Long, BigDecimal> getExactSplits() {
        return exactSplits;
    }

    public void setExactSplits(Map<Long, BigDecimal> exactSplits) {
        this.exactSplits = exactSplits;
    }

    public BigDecimal getPayerFixedAmount() {
        return payerFixedAmount;
    }

    public void setPayerFixedAmount(BigDecimal payerFixedAmount) {
        this.payerFixedAmount = payerFixedAmount;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }
}
