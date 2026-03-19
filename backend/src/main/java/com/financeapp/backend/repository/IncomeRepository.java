package com.financeapp.backend.repository;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.Income;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findAllByUserAndReceiveDateBetweenOrderByReceiveDateAsc(AppUser user, LocalDate start, LocalDate end);
    Optional<Income> findByIdAndUser(Long id, AppUser user);
    List<Income> findAllByUserAndRecurringTrueAndReceiveDateLessThanEqualOrderByReceiveDateAsc(AppUser user, LocalDate end);
}
