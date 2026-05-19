package com.broketogether.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemResponse(Long id,
                           String name,
                           BigDecimal price,
                           Boolean isChecked,
                           String addedByName,
                           String checkedByName,
                           LocalDateTime createdAt) {
}
