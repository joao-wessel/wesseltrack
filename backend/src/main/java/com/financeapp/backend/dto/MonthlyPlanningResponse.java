package com.financeapp.backend.dto;

import com.financeapp.backend.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyPlanningResponse(
        YearMonth month,
        BigDecimal goalAmount,
        BigDecimal creditLimit,
        BigDecimal debitLimit,
        BigDecimal pixLimit,
        BigDecimal cashLimit,
        Integer creditCardClosingDay
) {
    public BigDecimal limitFor(PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case CREDIT -> creditLimit;
            case DEBIT -> debitLimit;
            case PIX -> pixLimit;
            case CASH -> cashLimit;
        };
    }
}
