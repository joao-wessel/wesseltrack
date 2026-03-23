package com.financeapp.backend.repository;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.MonthlyGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.Optional;

public interface MonthlyGoalRepository extends JpaRepository<MonthlyGoal, Long> {
    Optional<MonthlyGoal> findByUserAndMonthKey(AppUser user, YearMonth monthKey);
    Optional<MonthlyGoal> findTopByUserAndMonthKeyLessThanEqualOrderByMonthKeyDesc(AppUser user, YearMonth monthKey);
}
