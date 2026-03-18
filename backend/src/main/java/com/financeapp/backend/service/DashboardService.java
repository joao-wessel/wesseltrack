package com.financeapp.backend.service;

import com.financeapp.backend.domain.PaymentMethod;
import com.financeapp.backend.dto.DashboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
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
        var planning = goalService.getPlanning(month);
        BigDecimal goal = planning.goalAmount();

        BigDecimal totalIncome = incomes.stream().map(i -> i.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenses.stream().map(e -> e.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, DashboardSummaryResponse.CategoryTotal> categoryTotals = new LinkedHashMap<>();
        for (var expense : expenses) {
            categoryTotals.compute(expense.category(), (key, existing) -> {
                BigDecimal total = (existing == null ? BigDecimal.ZERO : existing.total()).add(expense.amount());
                return new DashboardSummaryResponse.CategoryTotal(expense.category(), expense.categoryColor(), total);
            });
        }

        Map<PaymentMethod, BigDecimal> paymentMethodTotals = new LinkedHashMap<>();
        for (PaymentMethod method : Arrays.asList(PaymentMethod.CREDIT, PaymentMethod.DEBIT, PaymentMethod.PIX, PaymentMethod.CASH)) {
            paymentMethodTotals.put(method, BigDecimal.ZERO);
        }
        for (var expense : expenses) {
            paymentMethodTotals.merge(expense.paymentMethod(), expense.amount(), BigDecimal::add);
        }

        List<DashboardSummaryResponse.PaymentMethodUsage> usages = paymentMethodTotals.entrySet().stream()
                .map(entry -> {
                    BigDecimal limit = planning.limitFor(entry.getKey());
                    BigDecimal spent = entry.getValue();
                    return new DashboardSummaryResponse.PaymentMethodUsage(
                            labelFor(entry.getKey()),
                            spent,
                            limit,
                            limit.subtract(spent)
                    );
                })
                .toList();

        DashboardSummaryResponse.PaymentMethodUsage creditUsage = usages.stream()
                .filter(item -> item.method().equals(labelFor(PaymentMethod.CREDIT)))
                .findFirst()
                .orElse(new DashboardSummaryResponse.PaymentMethodUsage(labelFor(PaymentMethod.CREDIT), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        return new DashboardSummaryResponse(
                month.toString(),
                totalIncome,
                totalExpenses,
                goal,
                totalIncome.subtract(totalExpenses).subtract(goal),
                creditUsage,
                List.copyOf(categoryTotals.values()),
                usages,
                incomes,
                expenses
        );
    }

    private String labelFor(PaymentMethod method) {
        return switch (method) {
            case CREDIT -> "Crédito";
            case DEBIT -> "Débito";
            case PIX -> "PIX";
            case CASH -> "Dinheiro";
        };
    }
}
