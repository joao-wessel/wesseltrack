package com.financeapp.backend.service;

import com.financeapp.backend.domain.*;
import com.financeapp.backend.dto.ExpenseRequest;
import com.financeapp.backend.dto.ExpenseResponse;
import com.financeapp.backend.repository.ExpenseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CurrentUserService currentUserService;
    private final CategoryService categoryService;

    public List<ExpenseResponse> list(YearMonth month) {
        AppUser user = currentUserService.requireCurrentUser();
        return expenseRepository.findAllByUserAndDueDateBetweenOrderByDueDateAsc(user, month.atDay(1), month.atEndOfMonth()).stream()
                .map(this::map)
                .toList();
    }

    public List<ExpenseResponse> create(ExpenseRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        Category category = categoryService.requireOwnedCategory(request.categoryId(), user);

        if (request.type() == ExpenseType.INSTALLMENT) {
            return createInstallments(request, user, category);
        }

        Expense expense = expenseRepository.save(Expense.builder()
                .user(user)
                .category(category)
                .type(request.type())
                .paymentMethod(request.paymentMethod())
                .paymentSource(resolvePaymentSource(request.paymentMethod()))
                .amount(request.amount())
                .originalAmount(request.amount())
                .description(request.description().trim())
                .dueDate(request.dueDate())
                .recurring(request.type() == ExpenseType.FIXED && request.recurring())
                .installmentNumber(1)
                .installmentCount(1)
                .build());
        return List.of(map(expense));
    }

    public ExpenseResponse update(Long id, ExpenseRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Despesa não encontrada."));

        if (expense.getType() == ExpenseType.INSTALLMENT && expense.getInstallmentCount() > 1) {
            throw new IllegalArgumentException("Edite parcelas individualmente apenas quando necessário. O lote parcelado não é recalculado retroativamente.");
        }

        Category category = categoryService.requireOwnedCategory(request.categoryId(), user);
        expense.setCategory(category);
        expense.setType(request.type());
        expense.setPaymentMethod(request.paymentMethod());
        expense.setPaymentSource(resolvePaymentSource(request.paymentMethod()));
        expense.setAmount(request.amount());
        expense.setOriginalAmount(request.type() == ExpenseType.INSTALLMENT ? expense.getOriginalAmount() : request.amount());
        expense.setDescription(request.description().trim());
        expense.setDueDate(request.dueDate());
        expense.setRecurring(request.type() == ExpenseType.FIXED && request.recurring());

        if (request.type() != ExpenseType.INSTALLMENT) {
            expense.setInstallmentNumber(1);
            expense.setInstallmentCount(1);
            expense.setInstallmentGroup(null);
        }

        return map(expenseRepository.save(expense));
    }

    public void delete(Long id) {
        AppUser user = currentUserService.requireCurrentUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Despesa não encontrada."));
        expenseRepository.delete(expense);
    }

    public int closeMonth(YearMonth month) {
        AppUser user = currentUserService.requireCurrentUser();
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        YearMonth nextMonth = month.plusMonths(1);

        List<Expense> recurringFixedExpenses = expenseRepository
                .findAllByUserAndDueDateBetweenAndTypeAndRecurringTrueOrderByDueDateAsc(user, monthStart, monthEnd, ExpenseType.FIXED);

        int created = 0;
        for (Expense source : recurringFixedExpenses) {
            LocalDate nextDueDate = source.getDueDate().plusMonths(1);
            boolean exists = expenseRepository.existsByUserAndCategoryAndTypeAndPaymentMethodAndPaymentSourceAndAmountAndDescriptionAndDueDate(
                    user,
                    source.getCategory(),
                    source.getType(),
                    source.getPaymentMethod(),
                    source.getPaymentSource(),
                    source.getAmount(),
                    source.getDescription(),
                    nextDueDate
            );

            if (exists || !YearMonth.from(nextDueDate).equals(nextMonth)) {
                continue;
            }

            expenseRepository.save(Expense.builder()
                    .user(user)
                    .category(source.getCategory())
                    .type(source.getType())
                    .paymentMethod(source.getPaymentMethod())
                    .paymentSource(source.getPaymentSource())
                    .amount(source.getAmount())
                    .originalAmount(source.getOriginalAmount())
                    .description(source.getDescription())
                    .dueDate(nextDueDate)
                    .recurring(true)
                    .installmentNumber(1)
                    .installmentCount(1)
                    .build());
            created++;
        }

        return created;
    }

    private List<ExpenseResponse> createInstallments(ExpenseRequest request, AppUser user, Category category) {
        int installmentCount = request.installmentCount() == null ? 1 : request.installmentCount();
        BigDecimal perInstallment = request.amount().divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_UP);
        String group = UUID.randomUUID().toString();
        List<ExpenseResponse> responses = new ArrayList<>();

        for (int i = 0; i < installmentCount; i++) {
            Expense expense = expenseRepository.save(Expense.builder()
                    .user(user)
                    .category(category)
                    .type(ExpenseType.INSTALLMENT)
                    .paymentMethod(request.paymentMethod())
                    .paymentSource(resolvePaymentSource(request.paymentMethod()))
                    .amount(perInstallment)
                    .originalAmount(request.amount())
                    .description(request.description().trim())
                    .dueDate(request.dueDate().plusMonths(i))
                    .recurring(false)
                    .installmentNumber(i + 1)
                    .installmentCount(installmentCount)
                    .installmentGroup(group)
                    .build());
            responses.add(map(expense));
        }

        return responses;
    }

    private ExpenseResponse map(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getCategory().getName(),
                expense.getCategory().getColor(),
                expense.getType(),
                expense.getPaymentMethod(),
                expense.getPaymentSource(),
                expense.getAmount(),
                expense.getOriginalAmount(),
                expense.getDueDate(),
                expense.isRecurring(),
                expense.getInstallmentNumber(),
                expense.getInstallmentCount()
        );
    }

    private String resolvePaymentSource(PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case CREDIT -> "Cartao de credito";
            case DEBIT -> "Cartao de debito";
            case PIX -> "PIX";
            case CASH -> "Dinheiro";
        };
    }
}
