package com.financeapp.backend.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        String month,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal goalAmount,
        BigDecimal netBalance,
        BigDecimal debitExpenses,
        BigDecimal creditExpenses,
        java.util.List<CategoryTotal> byCategory,
        java.util.List<PaymentSourceTotal> byPaymentSource,
        java.util.List<IncomeResponse> incomes,
        java.util.List<ExpenseResponse> expenses
) {
    public record CategoryTotal(String category, String color, BigDecimal total) {}
    public record PaymentSourceTotal(String source, BigDecimal total) {}
}
