package com.financeapp.backend.service;

import com.financeapp.backend.domain.PaymentMethod;
import com.financeapp.backend.dto.DashboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final GoalService goalService;

    public DashboardSummaryResponse getMonthlySummary(YearMonth month) {
        var incomes = incomeService.list(month);
        var expenses = expenseService.list(month);
        BigDecimal goal = goalService.getGoal(month);

        BigDecimal totalIncome = incomes.stream().map(i -> i.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenses.stream().map(e -> e.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debitExpenses = expenses.stream()
                .filter(e -> e.paymentMethod() == PaymentMethod.DEBIT || e.paymentMethod() == PaymentMethod.PIX || e.paymentMethod() == PaymentMethod.CASH)
                .map(e -> e.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditExpenses = expenses.stream()
                .filter(e -> e.paymentMethod() == PaymentMethod.CREDIT)
                .map(e -> e.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, DashboardSummaryResponse.CategoryTotal> categoryTotals = new LinkedHashMap<>();
        for (var expense : expenses) {
            categoryTotals.compute(expense.category(), (key, existing) -> {
                BigDecimal total = (existing == null ? BigDecimal.ZERO : existing.total()).add(expense.amount());
                return new DashboardSummaryResponse.CategoryTotal(expense.category(), expense.categoryColor(), total);
            });
        }

        Map<String, BigDecimal> paymentSourceMap = new LinkedHashMap<>();
        for (var expense : expenses) {
            paymentSourceMap.merge(expense.paymentSource(), expense.amount(), BigDecimal::add);
        }

        return new DashboardSummaryResponse(
                month.toString(),
                totalIncome,
                totalExpenses,
                goal,
                totalIncome.subtract(totalExpenses).subtract(goal),
                debitExpenses,
                creditExpenses,
                List.copyOf(categoryTotals.values()),
                paymentSourceMap.entrySet().stream().map(e -> new DashboardSummaryResponse.PaymentSourceTotal(e.getKey(), e.getValue())).toList(),
                incomes,
                expenses
        );
    }
}
