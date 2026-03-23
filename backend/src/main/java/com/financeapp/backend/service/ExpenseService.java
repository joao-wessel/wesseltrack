package com.financeapp.backend.service;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.Category;
import com.financeapp.backend.domain.Expense;
import com.financeapp.backend.domain.ExpenseType;
import com.financeapp.backend.domain.PaymentMethod;
import com.financeapp.backend.dto.ExpenseRequest;
import com.financeapp.backend.dto.ExpenseResponse;
import com.financeapp.backend.repository.ExpenseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CurrentUserService currentUserService;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(YearMonth month) {
        AppUser user = currentUserService.requireCurrentUser();
        List<Expense> actual = expenseRepository.findAllByUserAndDueDateBetweenOrderByDueDateAsc(user, month.atDay(1), month.atEndOfMonth());
        List<Expense> recurringFixed = expenseRepository.findAllByUserAndRecurringTrueAndTypeAndDueDateLessThanEqualOrderByDueDateAsc(user, ExpenseType.FIXED, month.atEndOfMonth());

        List<ExpenseResponse> responses = new ArrayList<>(actual.stream().map(this::map).toList());
        for (Expense expense : recurringFixed) {
            if (!isRecurringActiveForMonth(expense, month)) {
                continue;
            }

            if (!expense.getDueDate().isBefore(month.atDay(1))) {
                continue;
            }

            LocalDate projectedDate = month.atDay(1).withDayOfMonth(Math.min(expense.getDueDate().getDayOfMonth(), month.lengthOfMonth()));
            boolean exists = actual.stream().anyMatch(current ->
                    current.getType() == ExpenseType.FIXED
                            && Objects.equals(current.getCategory(), expense.getCategory())
                            && current.getPaymentMethod() == expense.getPaymentMethod()
                            && Objects.equals(current.getPaymentSource(), expense.getPaymentSource())
                            && current.getAmount().compareTo(expense.getAmount()) == 0
                            && current.getDescription().equals(expense.getDescription())
                            && current.getDueDate().equals(projectedDate)
            );

            if (!exists) {
                responses.add(new ExpenseResponse(
                        expense.getId(),
                        expense.getDescription(),
                        expense.getCategory() == null ? "Sem categoria" : expense.getCategory().getName(),
                        expense.getCategory() == null ? "#94a3b8" : expense.getCategory().getColor(),
                        expense.getType(),
                        expense.getPaymentMethod(),
                        expense.getPaymentSource(),
                        expense.getAmount(),
                        expense.getOriginalAmount(),
                        projectedDate,
                        true,
                        1,
                        1
                ));
            }
        }

        responses.sort(Comparator.comparing(ExpenseResponse::dueDate));
        return responses;
    }

    @Transactional
    public List<ExpenseResponse> create(ExpenseRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        Category category = requireCategory(request, user);

        if (request.type() == ExpenseType.INSTALLMENT) {
            return createInstallments(request, user, category);
        }

        Expense expense = expenseRepository.save(Expense.builder()
                .user(user)
                .category(category)
                .type(request.type())
                .paymentMethod(request.paymentMethod())
                .paymentSource(derivePaymentSource(request.paymentMethod()))
                .amount(request.amount())
                .originalAmount(request.amount())
                .description(request.description().trim())
                .dueDate(request.dueDate())
                .endDate(null)
                .recurring(request.type() == ExpenseType.FIXED && request.recurring())
                .installmentNumber(1)
                .installmentCount(1)
                .build());
        return List.of(map(expense));
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Despesa nÃ£o encontrada."));

        if (expense.getType() == ExpenseType.INSTALLMENT && expense.getInstallmentCount() > 1) {
            throw new IllegalArgumentException("Edite parcelas individualmente apenas quando necessÃ¡rio. O lote parcelado nÃ£o Ã© recalculado retroativamente.");
        }

        Category category = requireCategory(request, user);
        if (expense.isRecurring() && expense.getType() == ExpenseType.FIXED && request.type() == ExpenseType.FIXED && request.recurring()) {
            return updateRecurringFixedExpense(expense, request, category);
        }

        expense.setCategory(category);
        expense.setType(request.type());
        expense.setPaymentMethod(request.paymentMethod());
        expense.setPaymentSource(derivePaymentSource(request.paymentMethod()));
        expense.setAmount(request.amount());
        expense.setOriginalAmount(request.type() == ExpenseType.INSTALLMENT ? expense.getOriginalAmount() : request.amount());
        expense.setDescription(request.description().trim());
        expense.setDueDate(request.dueDate());
        expense.setEndDate(null);
        expense.setRecurring(request.type() == ExpenseType.FIXED && request.recurring());

        if (request.type() != ExpenseType.INSTALLMENT) {
            expense.setInstallmentNumber(1);
            expense.setInstallmentCount(1);
            expense.setInstallmentGroup(null);
        }

        return map(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long id, YearMonth effectiveMonth) {
        AppUser user = currentUserService.requireCurrentUser();
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Despesa nÃ£o encontrada."));

        if (!(expense.isRecurring() && expense.getType() == ExpenseType.FIXED)) {
            expenseRepository.delete(expense);
            return;
        }

        LocalDate effectiveStart = effectiveMonth.atDay(1);
        if (!effectiveStart.isAfter(expense.getDueDate())) {
            expenseRepository.delete(expense);
            return;
        }

        expense.setEndDate(effectiveStart.minusDays(1));
        expenseRepository.save(expense);
    }

    private ExpenseResponse updateRecurringFixedExpense(Expense expense, ExpenseRequest request, Category category) {
        LocalDate effectiveStart = request.dueDate();
        LocalDate originalStart = expense.getDueDate();

        if (!effectiveStart.isAfter(originalStart)) {
            expense.setCategory(category);
            expense.setPaymentMethod(request.paymentMethod());
            expense.setPaymentSource(derivePaymentSource(request.paymentMethod()));
            expense.setAmount(request.amount());
            expense.setOriginalAmount(request.amount());
            expense.setDescription(request.description().trim());
            expense.setDueDate(request.dueDate());
            expense.setRecurring(true);
            return map(expenseRepository.save(expense));
        }

        Expense updatedVersion = Expense.builder()
                .user(expense.getUser())
                .category(category)
                .type(ExpenseType.FIXED)
                .paymentMethod(request.paymentMethod())
                .paymentSource(derivePaymentSource(request.paymentMethod()))
                .amount(request.amount())
                .originalAmount(request.amount())
                .description(request.description().trim())
                .dueDate(request.dueDate())
                .endDate(expense.getEndDate())
                .recurring(true)
                .installmentNumber(1)
                .installmentCount(1)
                .build();

        expense.setEndDate(effectiveStart.minusDays(1));
        expenseRepository.save(expense);
        return map(expenseRepository.save(updatedVersion));
    }

    private List<ExpenseResponse> createInstallments(ExpenseRequest request, AppUser user, Category category) {
        int installmentCount = request.installmentCount() == null ? 1 : request.installmentCount();
        LocalDate firstInstallmentDate = request.firstInstallmentNextMonth()
                ? request.dueDate().plusMonths(1)
                : request.dueDate();
        BigDecimal[] division = request.amount().divideAndRemainder(BigDecimal.valueOf(installmentCount));
        BigDecimal baseInstallment = division[0].setScale(2, RoundingMode.DOWN);
        BigDecimal totalAssigned = baseInstallment.multiply(BigDecimal.valueOf(installmentCount));
        BigDecimal remainder = request.amount().subtract(totalAssigned).setScale(2, RoundingMode.HALF_UP);
        String group = UUID.randomUUID().toString();
        List<ExpenseResponse> responses = new ArrayList<>();

        for (int i = 0; i < installmentCount; i++) {
            BigDecimal installmentAmount = baseInstallment;
            if (i == installmentCount - 1) {
                installmentAmount = installmentAmount.add(remainder);
            }

            Expense expense = expenseRepository.save(Expense.builder()
                    .user(user)
                    .category(category)
                    .type(ExpenseType.INSTALLMENT)
                    .paymentMethod(request.paymentMethod())
                    .paymentSource(derivePaymentSource(request.paymentMethod()))
                    .amount(installmentAmount)
                    .originalAmount(request.amount())
                    .description(request.description().trim())
                    .dueDate(firstInstallmentDate.plusMonths(i))
                    .endDate(null)
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
                expense.getCategory() == null ? "Sem categoria" : expense.getCategory().getName(),
                expense.getCategory() == null ? "#94a3b8" : expense.getCategory().getColor(),
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

    private Category requireCategory(ExpenseRequest request, AppUser user) {
        if (request.categoryId() == null) {
            if (request.type() == ExpenseType.VARIABLE) {
                return categoryService.findOrCreateUncategorized(user);
            }
            throw new IllegalArgumentException("Categoria obrigatÃ³ria para despesas fixas e parceladas.");
        }

        return categoryService.requireOwnedCategory(request.categoryId(), user);
    }

    private String derivePaymentSource(PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case CREDIT -> "CrÃ©dito";
            case DEBIT -> "DÃ©bito";
            case PIX -> "PIX";
            case CASH -> "Dinheiro";
        };
    }

    private boolean isRecurringActiveForMonth(Expense expense, YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        return !expense.getDueDate().isAfter(monthEnd)
                && (expense.getEndDate() == null || !expense.getEndDate().isBefore(monthStart));
    }
}
