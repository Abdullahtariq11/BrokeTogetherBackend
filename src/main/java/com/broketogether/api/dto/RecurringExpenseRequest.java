package com.broketogether.api.dto;

import com.broketogether.api.model.RecurringFrequency;
import com.broketogether.api.model.RecurringSplitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class RecurringExpenseRequest {

    @NotNull(message = "Home ID is required")
    private Long homeId;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String category;

    @NotNull(message = "Frequency is required")
    private RecurringFrequency frequency;

    private RecurringSplitType splitType = RecurringSplitType.SPLIT;

    public Long getHomeId() { return homeId; }
    public void setHomeId(Long homeId) { this.homeId = homeId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public RecurringFrequency getFrequency() { return frequency; }
    public void setFrequency(RecurringFrequency frequency) { this.frequency = frequency; }

    public RecurringSplitType getSplitType() { return splitType; }
    public void setSplitType(RecurringSplitType splitType) { this.splitType = splitType; }
}
