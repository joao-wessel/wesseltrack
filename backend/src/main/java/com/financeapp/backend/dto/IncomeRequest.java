package com.financeapp.backend.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeRequest(
        @NotBlank @Size(max = 120) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        LocalDate receiveDate,
        @Min(1) @Max(31) Integer expectedDay,
        boolean recurring
) {
}
