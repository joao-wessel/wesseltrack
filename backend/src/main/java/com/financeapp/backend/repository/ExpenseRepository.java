package com.financeapp.backend.repository;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.Category;
import com.financeapp.backend.domain.Expense;
import com.financeapp.backend.domain.ExpenseType;
import com.financeapp.backend.domain.PaymentMethod;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    @EntityGraph(attributePaths = "category")
    List<Expense> findAllByUserAndDueDateBetweenOrderByDueDateAsc(AppUser user, LocalDate start, LocalDate end);

    Optional<Expense> findByIdAndUser(Long id, AppUser user);

    long countByUserAndCategory(AppUser user, Category category);

    @EntityGraph(attributePaths = "category")
    List<Expense> findAllByUserAndRecurringTrueAndTypeAndDueDateLessThanEqualOrderByDueDateAsc(AppUser user, ExpenseType type, LocalDate end);

    @EntityGraph(attributePaths = "category")
    List<Expense> findAllByUserAndDueDateBetweenAndTypeAndRecurringTrueOrderByDueDateAsc(AppUser user, LocalDate start, LocalDate end, ExpenseType type);

    boolean existsByUserAndCategoryAndTypeAndPaymentMethodAndPaymentSourceAndAmountAndDescriptionAndDueDate(
            AppUser user,
            Category category,
            ExpenseType type,
            PaymentMethod paymentMethod,
            String paymentSource,
            BigDecimal amount,
            String description,
            LocalDate dueDate
    );
}
