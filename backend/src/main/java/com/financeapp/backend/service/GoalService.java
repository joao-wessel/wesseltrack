package com.financeapp.backend.service;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.MonthlyGoal;
import com.financeapp.backend.dto.MonthlyGoalRequest;
import com.financeapp.backend.repository.MonthlyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final MonthlyGoalRepository monthlyGoalRepository;
    private final CurrentUserService currentUserService;

    public BigDecimal getGoal(YearMonth month) {
        AppUser user = currentUserService.requireCurrentUser();
        return monthlyGoalRepository.findByUserAndMonthKey(user, month)
                .map(MonthlyGoal::getAmount)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal save(MonthlyGoalRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        MonthlyGoal goal = monthlyGoalRepository.findByUserAndMonthKey(user, request.month())
                .orElse(MonthlyGoal.builder().user(user).monthKey(request.month()).amount(BigDecimal.ZERO).build());
        goal.setAmount(request.amount());
        return monthlyGoalRepository.save(goal).getAmount();
    }
}
