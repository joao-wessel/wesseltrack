package com.financeapp.backend.service;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.MonthlyGoal;
import com.financeapp.backend.domain.MonthlyPaymentLimit;
import com.financeapp.backend.domain.PaymentMethod;
import com.financeapp.backend.dto.MonthlyGoalRequest;
import com.financeapp.backend.dto.MonthlyPlanningResponse;
import com.financeapp.backend.dto.PlanningSettingsRequest;
import com.financeapp.backend.dto.PlanningSettingsResponse;
import com.financeapp.backend.repository.MonthlyGoalRepository;
import com.financeapp.backend.repository.MonthlyPaymentLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoalService {

    private static final YearMonth SETTINGS_MONTH = YearMonth.of(2000, 1);

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
        BigDecimal goalAmount = getGoal(SETTINGS_MONTH);
        Map<PaymentMethod, BigDecimal> limits = loadLimits(user, SETTINGS_MONTH);

        return new MonthlyPlanningResponse(
                month,
                goalAmount,
                limits.get(PaymentMethod.CREDIT),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    public PlanningSettingsResponse getSettings() {
        AppUser user = currentUserService.requireCurrentUser();
        BigDecimal goalAmount = getGoal(SETTINGS_MONTH);
        Map<PaymentMethod, BigDecimal> limits = loadLimits(user, SETTINGS_MONTH);
        return new PlanningSettingsResponse(goalAmount, limits.get(PaymentMethod.CREDIT));
    }

    public PlanningSettingsResponse saveSettings(PlanningSettingsRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        save(new MonthlyGoalRequest(SETTINGS_MONTH, request.reserveGoal()));
        saveLimit(user, SETTINGS_MONTH, PaymentMethod.CREDIT, request.creditLimit());
        saveLimit(user, SETTINGS_MONTH, PaymentMethod.DEBIT, BigDecimal.ZERO);
        saveLimit(user, SETTINGS_MONTH, PaymentMethod.PIX, BigDecimal.ZERO);
        saveLimit(user, SETTINGS_MONTH, PaymentMethod.CASH, BigDecimal.ZERO);
        return getSettings();
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
