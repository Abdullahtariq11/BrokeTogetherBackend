package com.broketogether.api.dto;

import java.math.BigDecimal;

public record ItemRequest(String name, BigDecimal price) {
}
