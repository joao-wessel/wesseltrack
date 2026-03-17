package com.financeapp.backend.dto;

import com.financeapp.backend.domain.ExpenseType;
import com.financeapp.backend.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        String description,
        String category,
        String categoryColor,
        ExpenseType type,
        PaymentMethod paymentMethod,
        String paymentSource,
        BigDecimal amount,
        BigDecimal originalAmount,
        LocalDate dueDate,
        boolean recurring,
        Integer installmentNumber,
        Integer installmentCount
) {
}
