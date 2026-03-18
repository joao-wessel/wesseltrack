package com.financeapp.backend.service;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.MonthlyGoal;
import com.financeapp.backend.domain.MonthlyPaymentLimit;
import com.financeapp.backend.domain.PaymentMethod;
import com.financeapp.backend.dto.MonthlyGoalRequest;
import com.financeapp.backend.dto.MonthlyPlanningRequest;
import com.financeapp.backend.dto.MonthlyPlanningResponse;
import com.financeapp.backend.repository.MonthlyGoalRepository;
import com.financeapp.backend.repository.MonthlyPaymentLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.time.YearMonth;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final MonthlyGoalRepository monthlyGoalRepository;
    private final MonthlyPaymentLimitRepository monthlyPaymentLimitRepository;
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

    public MonthlyPlanningResponse getPlanning(YearMonth month) {
        AppUser user = currentUserService.requireCurrentUser();
        BigDecimal goalAmount = getGoal(month);
        Map<PaymentMethod, BigDecimal> limits = loadLimits(user, month);

        return new MonthlyPlanningResponse(
                month,
                goalAmount,
                limits.get(PaymentMethod.CREDIT),
                limits.get(PaymentMethod.DEBIT),
                limits.get(PaymentMethod.PIX),
                limits.get(PaymentMethod.CASH)
        );
    }

    public MonthlyPlanningResponse savePlanning(MonthlyPlanningRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        save(new MonthlyGoalRequest(request.month(), request.goalAmount()));
        saveLimit(user, request.month(), PaymentMethod.CREDIT, request.creditLimit());
        saveLimit(user, request.month(), PaymentMethod.DEBIT, request.debitLimit());
        saveLimit(user, request.month(), PaymentMethod.PIX, request.pixLimit());
        saveLimit(user, request.month(), PaymentMethod.CASH, request.cashLimit());
        return getPlanning(request.month());
    }

    private void saveLimit(AppUser user, YearMonth month, PaymentMethod paymentMethod, BigDecimal amount) {
        MonthlyPaymentLimit limit = monthlyPaymentLimitRepository.findByUserAndMonthKeyAndPaymentMethod(user, month, paymentMethod)
                .orElse(MonthlyPaymentLimit.builder()
                        .user(user)
                        .monthKey(month)
                        .paymentMethod(paymentMethod)
                        .amount(BigDecimal.ZERO)
                        .build());
        limit.setAmount(amount);
        monthlyPaymentLimitRepository.save(limit);
    }

    private Map<PaymentMethod, BigDecimal> loadLimits(AppUser user, YearMonth month) {
        Map<PaymentMethod, BigDecimal> limits = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            limits.put(method, BigDecimal.ZERO);
        }

        for (MonthlyPaymentLimit limit : monthlyPaymentLimitRepository.findAllByUserAndMonthKey(user, month)) {
            limits.put(limit.getPaymentMethod(), limit.getAmount());
        }

        return limits;
    }
}
