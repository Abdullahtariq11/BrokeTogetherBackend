package com.broketogether.api.dto;

import com.broketogether.api.dto.enums.SplitType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public class ConvertExpenseRequest {

    @NotNull(message = "Split type is required")
    private SplitType splitType = SplitType.EQUAL;

    private Set<Long> userIds;

    private Map<Long, BigDecimal> exactSplits;

    private BigDecimal payerFixedAmount;

    public ConvertExpenseRequest() {}

    public SplitType getSplitType() { return splitType; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }

    public Set<Long> getUserIds() { return userIds; }
    public void setUserIds(Set<Long> userIds) { this.userIds = userIds; }

    public Map<Long, BigDecimal> getExactSplits() { return exactSplits; }
    public void setExactSplits(Map<Long, BigDecimal> exactSplits) { this.exactSplits = exactSplits; }

    public BigDecimal getPayerFixedAmount() { return payerFixedAmount; }
    public void setPayerFixedAmount(BigDecimal payerFixedAmount) { this.payerFixedAmount = payerFixedAmount; }
}
