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
    List<Expense> findAllByUserAndDueDateBetweenAndCreditCardBillPaymentFalseOrderByDueDateAsc(AppUser user, LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = "category")
    Optional<Expense> findByIdAndUser(Long id, AppUser user);

    long countByUserAndCategory(AppUser user, Category category);

    @EntityGraph(attributePaths = "category")
    List<Expense> findAllByUserAndRecurringTrueAndTypeAndDueDateLessThanEqualAndCreditCardBillPaymentFalseOrderByDueDateAsc(AppUser user, ExpenseType type, LocalDate end);

    @EntityGraph(attributePaths = "category")
    List<Expense> findAllByUserAndDueDateBetweenAndTypeAndRecurringTrueAndCreditCardBillPaymentFalseOrderByDueDateAsc(AppUser user, LocalDate start, LocalDate end, ExpenseType type);

    @EntityGraph(attributePaths = "category")
    List<Expense> findAllByUserAndDueDateBetweenAndPaymentMethodAndCreditCardBillPaymentFalseOrderByDueDateAsc(
            AppUser user,
            LocalDate start,
            LocalDate end,
            PaymentMethod paymentMethod
    );

    List<Expense> findAllByUserAndDueDateBetweenAndCreditCardBillPaymentTrueOrderByDueDateAsc(AppUser user, LocalDate start, LocalDate end);

    Optional<Expense> findByUserAndCreditCardBillPaymentTrueAndCreditCardStatementMonth(AppUser user, java.time.YearMonth statementMonth);

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
