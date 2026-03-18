package com.financeapp.backend.repository;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.MonthlyPaymentLimit;
import com.financeapp.backend.domain.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface MonthlyPaymentLimitRepository extends JpaRepository<MonthlyPaymentLimit, Long> {
    List<MonthlyPaymentLimit> findAllByUserAndMonthKey(AppUser user, YearMonth monthKey);
    Optional<MonthlyPaymentLimit> findByUserAndMonthKeyAndPaymentMethod(AppUser user, YearMonth monthKey, PaymentMethod paymentMethod);
}
