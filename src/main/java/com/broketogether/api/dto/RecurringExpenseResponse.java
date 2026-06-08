package com.broketogether.api.dto;

import com.broketogether.api.model.RecurringFrequency;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpenseResponse(
    Long id,
    Long homeId,
    String description,
    BigDecimal amount,
    String category,
    Long payerId,
    RecurringFrequency frequency,
    LocalDate nextDueDate,
    boolean active
) {}
