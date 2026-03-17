package com.financeapp.backend.repository;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.Category;
import com.financeapp.backend.domain.Expense;
import com.financeapp.backend.domain.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByUserAndDueDateBetweenOrderByDueDateAsc(AppUser user, LocalDate start, LocalDate end);
    Optional<Expense> findByIdAndUser(Long id, AppUser user);
    long countByUserAndCategory(AppUser user, Category category);
    List<Expense> findAllByUserAndDueDateBetweenAndTypeAndRecurringTrueOrderByDueDateAsc(
            AppUser user,
            LocalDate start,
            LocalDate end,
            ExpenseType type
    );
    boolean existsByUserAndCategoryAndTypeAndPaymentMethodAndPaymentSourceAndAmountAndDescriptionAndDueDate(
            AppUser user,
            Category category,
            ExpenseType type,
            com.financeapp.backend.domain.PaymentMethod paymentMethod,
            String paymentSource,
            java.math.BigDecimal amount,
            String description,
            LocalDate dueDate
    );
}
