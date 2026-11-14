package com.broketogether.api.dto;

import java.math.BigDecimal;

public record ReceiptScanResponse(
    /** Extracted total in CAD. Null if the receipt could not be parsed. */
    BigDecimal amount,
    /** Human-readable note from the model (e.g. store name, confidence note). */
    String note
) {}
