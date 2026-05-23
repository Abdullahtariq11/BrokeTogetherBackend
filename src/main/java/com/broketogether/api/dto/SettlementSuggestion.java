package com.broketogether.api.dto;

import java.math.BigDecimal;

public record SettlementSuggestion(Long debtorId,
                                   String debtorName,
                                   Long creditorId,
                                   String creditorName,
                                   BigDecimal amount) {
}
