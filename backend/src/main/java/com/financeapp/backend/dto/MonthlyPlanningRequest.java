package com.financeapp.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyPlanningRequest(
        @NotNull YearMonth month,
        @NotNull @DecimalMin(value = "0.00") BigDecimal goalAmount,
        @NotNull @DecimalMin(value = "0.00") BigDecimal creditLimit,
        @NotNull @DecimalMin(value = "0.00") BigDecimal debitLimit,
        @NotNull @DecimalMin(value = "0.00") BigDecimal pixLimit,
        @NotNull @DecimalMin(value = "0.00") BigDecimal cashLimit
) {
}
