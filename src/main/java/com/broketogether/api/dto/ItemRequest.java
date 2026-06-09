package com.broketogether.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemRequest(
    @NotBlank(message = "Item name is required")
    @Size(max = 255, message = "Item name must not exceed 255 characters")
    String name,

    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    BigDecimal price,  // optional — kept nullable for items without a price

    @NotNull(message = "Home ID is required")
    Long homeId
) {}
