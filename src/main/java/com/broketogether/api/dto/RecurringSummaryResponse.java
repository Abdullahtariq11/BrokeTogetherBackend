package com.broketogether.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RecurringSummaryResponse(
    BigDecimal totalMonthlyAmount,
    List<NextDueItem> nextDueItems
) {
    public record NextDueItem(Long id, String description, BigDecimal amount, LocalDate nextDueDate) {}
}
