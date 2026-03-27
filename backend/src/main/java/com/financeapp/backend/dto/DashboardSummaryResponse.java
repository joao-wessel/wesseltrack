package com.financeapp.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardSummaryResponse(
        String month,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal nonCreditExpenses,
        BigDecimal creditPurchases,
        BigDecimal billPayments,
        BigDecimal cashOutflow,
        BigDecimal goalAmount,
        BigDecimal netBalance,
        BigDecimal maxCreditCardBill,
        BigDecimal fixedExpensesOutsideCredit,
        Integer creditCardClosingDay,
        CreditCardStatementSummary currentStatement,
        CreditCardStatementSummary dueStatement,
        PaymentMethodUsage creditUsage,
        java.util.List<CategoryTotal> byCategory,
        java.util.List<PaymentMethodUsage> byPaymentMethod,
        java.util.List<IncomeResponse> incomes,
        java.util.List<ExpenseResponse> expenses
) {
    public record CategoryTotal(String category, String color, BigDecimal total) {}
    public record PaymentMethodUsage(String method, BigDecimal spent, BigDecimal limitAmount, BigDecimal remaining) {}
    public record CreditCardStatementSummary(
            String statementMonth,
            String dueMonth,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal amount,
            boolean paid,
            LocalDate paidAt,
            Long paymentExpenseId
    ) {}
}
