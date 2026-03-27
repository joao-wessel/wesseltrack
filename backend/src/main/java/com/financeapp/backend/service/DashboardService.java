package com.financeapp.backend.service;

import com.financeapp.backend.domain.ExpenseType;
import com.financeapp.backend.domain.PaymentMethod;
import com.financeapp.backend.dto.DashboardSummaryResponse;
import com.financeapp.backend.dto.ExpenseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
        BigDecimal goal = planning.goalAmount() == null ? BigDecimal.ZERO : planning.goalAmount();
        int creditCardClosingDay = planning.creditCardClosingDay() == null
                ? goalService.getCreditCardClosingDay()
                : planning.creditCardClosingDay();

        DashboardSummaryResponse.CreditCardStatementSummary currentStatement =
                buildStatementSummary(month.plusMonths(1), creditCardClosingDay);
        DashboardSummaryResponse.CreditCardStatementSummary dueStatement =
                buildStatementSummary(month.minusMonths(1), creditCardClosingDay);

        BigDecimal totalIncome = incomes.stream().map(income -> income.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenses.stream().map(expense -> expense.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal nonCreditExpenses = expenses.stream()
                .filter(expense -> expense.paymentMethod() != PaymentMethod.CREDIT)
                .map(ExpenseResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditPurchases = expenses.stream()
                .filter(expense -> expense.paymentMethod() == PaymentMethod.CREDIT)
                .map(ExpenseResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal billPayments = expenseService.totalBillPayments(month);
        BigDecimal cashOutflow = nonCreditExpenses.add(billPayments);
        BigDecimal fixedExpensesOutsideCredit = expenses.stream()
                .filter(expense -> expense.type() == ExpenseType.FIXED)
                .filter(expense -> expense.paymentMethod() != PaymentMethod.CREDIT)
                .map(ExpenseResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maxCreditCardBill = totalIncome.subtract(fixedExpensesOutsideCredit)
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        if (maxCreditCardBill.signum() < 0) {
            maxCreditCardBill = BigDecimal.ZERO;
        }

        Map<String, DashboardSummaryResponse.CategoryTotal> categoryTotals = new LinkedHashMap<>();
        for (var expense : expenses) {
            String category = expense.category() == null || expense.category().isBlank() ? "Sem categoria" : expense.category();
            String color = expense.categoryColor() == null || expense.categoryColor().isBlank() ? "#94a3b8" : expense.categoryColor();
            categoryTotals.compute(category, (key, existing) -> {
                BigDecimal total = (existing == null ? BigDecimal.ZERO : existing.total()).add(expense.amount());
                return new DashboardSummaryResponse.CategoryTotal(category, color, total);
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
                    if (limit == null) {
                        limit = BigDecimal.ZERO;
                    }
                    BigDecimal spent = entry.getValue();
                    return new DashboardSummaryResponse.PaymentMethodUsage(
                            labelFor(entry.getKey()),
                            spent,
                            limit,
                            limit.subtract(spent)
                    );
                })
                .toList();

        BigDecimal creditLimit = planning.limitFor(PaymentMethod.CREDIT) == null ? BigDecimal.ZERO : planning.limitFor(PaymentMethod.CREDIT);
        DashboardSummaryResponse.PaymentMethodUsage creditUsage = new DashboardSummaryResponse.PaymentMethodUsage(
                labelFor(PaymentMethod.CREDIT),
                currentStatement.amount(),
                creditLimit,
                creditLimit.subtract(currentStatement.amount())
        );

        return new DashboardSummaryResponse(
                month.toString(),
                totalIncome,
                totalExpenses,
                nonCreditExpenses,
                creditPurchases,
                billPayments,
                cashOutflow,
                goal,
                totalIncome.subtract(cashOutflow).subtract(goal),
                maxCreditCardBill,
                fixedExpensesOutsideCredit,
                creditCardClosingDay,
                currentStatement,
                dueStatement,
                creditUsage,
                List.copyOf(categoryTotals.values()),
                usages,
                incomes,
                expenses
        );
    }

    public DashboardSummaryResponse payDueStatement(YearMonth month) {
        int creditCardClosingDay = goalService.getCreditCardClosingDay();
        YearMonth statementMonth = month.minusMonths(1);
        DashboardSummaryResponse.CreditCardStatementSummary statement = buildStatementSummary(statementMonth, creditCardClosingDay);

        if (statement.amount().signum() <= 0) {
            throw new IllegalArgumentException("Nao ha fatura pendente para este mes.");
        }
        if (statement.paid()) {
            throw new IllegalArgumentException("A fatura deste periodo ja foi paga.");
        }

        expenseService.createCreditCardBillPayment(statementMonth, statement.amount(), LocalDate.now());
        return getMonthlySummary(month);
    }

    private DashboardSummaryResponse.CreditCardStatementSummary buildStatementSummary(YearMonth statementMonth, int closingDay) {
        LocalDate periodStart = resolveStatementPeriodStart(statementMonth, closingDay);
        LocalDate periodEnd = resolveStatementPeriodEnd(statementMonth, closingDay);
        BigDecimal amount = statementExpenses(statementMonth, periodStart, periodEnd).stream()
                .map(ExpenseResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ExpenseResponse payment = expenseService.findCreditCardBillPayment(statementMonth);

        return new DashboardSummaryResponse.CreditCardStatementSummary(
                statementMonth.toString(),
                statementMonth.plusMonths(1).toString(),
                periodStart,
                periodEnd,
                amount,
                payment != null,
                payment == null ? null : payment.dueDate(),
                payment == null ? null : payment.id()
        );
    }

    private List<ExpenseResponse> statementExpenses(YearMonth statementMonth, LocalDate periodStart, LocalDate periodEnd) {
        return Stream.concat(
                        expenseService.list(statementMonth.minusMonths(1)).stream(),
                        expenseService.list(statementMonth).stream()
                )
                .filter(expense -> expense.paymentMethod() == PaymentMethod.CREDIT)
                .filter(expense -> !expense.dueDate().isBefore(periodStart) && !expense.dueDate().isAfter(periodEnd))
                .toList();
    }

    private LocalDate resolveStatementPeriodStart(YearMonth statementMonth, int closingDay) {
        LocalDate previousClosing = resolveStatementPeriodEnd(statementMonth.minusMonths(1), closingDay);
        return previousClosing.plusDays(1);
    }

    private LocalDate resolveStatementPeriodEnd(YearMonth statementMonth, int closingDay) {
        return statementMonth.atDay(Math.min(Math.max(closingDay, 1), statementMonth.lengthOfMonth()));
    }

    private String labelFor(PaymentMethod method) {
        return switch (method) {
            case CREDIT -> "Credito";
            case DEBIT -> "Debito";
            case PIX -> "PIX";
            case CASH -> "Dinheiro";
        };
    }
}
