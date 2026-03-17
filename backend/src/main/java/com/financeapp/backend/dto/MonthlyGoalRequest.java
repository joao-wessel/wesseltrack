package com.financeapp.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyGoalRequest(
        @NotNull YearMonth month,
        @NotNull @DecimalMin(value = "0.00") BigDecimal amount
) {
}
