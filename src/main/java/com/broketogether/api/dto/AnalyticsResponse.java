package com.broketogether.api.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AnalyticsResponse(
    Map<String, BigDecimal> spendingByCategory,
    Map<String, BigDecimal> monthlyTotals,
    Map<String, BigDecimal> spendingByMember,
    BigDecimal largestExpenseAmount,
    String largestExpenseDescription
) {}
