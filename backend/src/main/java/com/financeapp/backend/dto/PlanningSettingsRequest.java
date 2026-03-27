package com.financeapp.backend.dto;

import java.math.BigDecimal;

public record PlanningSettingsRequest(
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin(value = "0.00") BigDecimal reserveGoal,
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin(value = "0.00") BigDecimal creditLimit,
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(31) Integer creditCardClosingDay
) {
}
