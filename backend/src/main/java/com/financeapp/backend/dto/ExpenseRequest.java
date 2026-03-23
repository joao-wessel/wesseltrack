package com.financeapp.backend.dto;

import com.financeapp.backend.domain.ExpenseType;
import com.financeapp.backend.domain.PaymentMethod;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotBlank @Size(max = 140) String description,
        Long categoryId,
        @NotNull ExpenseType type,
        @NotNull PaymentMethod paymentMethod,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull LocalDate dueDate,
        boolean recurring,
        @Min(1) @Max(60) Integer installmentCount,
        boolean firstInstallmentNextMonth
) {
}
