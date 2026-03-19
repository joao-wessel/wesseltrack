package com.financeapp.backend.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        String month,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal goalAmount,
        BigDecimal netBalance,
        BigDecimal maxCreditCardBill,
        BigDecimal fixedExpensesOutsideCredit,
        PaymentMethodUsage creditUsage,
        java.util.List<CategoryTotal> byCategory,
        java.util.List<PaymentMethodUsage> byPaymentMethod,
        java.util.List<IncomeResponse> incomes,
        java.util.List<ExpenseResponse> expenses
) {
    public record CategoryTotal(String category, String color, BigDecimal total) {}
    public record PaymentMethodUsage(String method, BigDecimal spent, BigDecimal limitAmount, BigDecimal remaining) {}
}
