package com.broketogether.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementHistoryResponse(
    Long expenseId,
    BigDecimal amount,
    String description,
    Long payerId,
    String payerName,
    LocalDateTime createdAt
) {}
