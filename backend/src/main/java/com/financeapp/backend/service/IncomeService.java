package com.financeapp.backend.service;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.Income;
import com.financeapp.backend.dto.IncomeRequest;
import com.financeapp.backend.dto.IncomeResponse;
import com.financeapp.backend.repository.IncomeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final CurrentUserService currentUserService;

    public List<IncomeResponse> list(YearMonth month) {
        AppUser user = currentUserService.requireCurrentUser();
        List<Income> actual = incomeRepository.findAllByUserAndReceiveDateBetweenOrderByReceiveDateAsc(user, month.atDay(1), month.atEndOfMonth());
        List<Income> recurring = incomeRepository.findAllByUserAndRecurringTrueAndReceiveDateLessThanEqualOrderByReceiveDateAsc(user, month.atEndOfMonth());

        Map<Long, IncomeResponse> responseMap = new LinkedHashMap<>();
        for (Income income : actual) {
            responseMap.put(income.getId(), map(income));
        }

        for (Income income : recurring) {
            if (!isRecurringActiveForMonth(income, month)) {
                continue;
            }

            if (!income.getReceiveDate().isBefore(month.atDay(1))) {
                continue;
            }

            LocalDate projectedDate = month.atDay(1).withDayOfMonth(Math.min(income.getExpectedDay(), month.lengthOfMonth()));
            responseMap.putIfAbsent(income.getId(), new IncomeResponse(
                    income.getId(),
                    income.getDescription(),
                    income.getAmount(),
                    projectedDate,
                    income.getExpectedDay(),
                    true
            ));
        }

        List<IncomeResponse> responses = new ArrayList<>(responseMap.values());
        responses.sort(Comparator.comparing(IncomeResponse::receiveDate));
        return responses;
    }

    public IncomeResponse create(IncomeRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        Income income = buildIncome(Income.builder().user(user).build(), request);
        return map(incomeRepository.save(income));
    }

    public IncomeResponse update(Long id, IncomeRequest request) {
        AppUser user = currentUserService.requireCurrentUser();
        Income income = incomeRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Receita nÃ£o encontrada."));

        if (income.isRecurring() && request.recurring()) {
            return updateRecurringIncome(income, request);
        }

        income = buildIncome(income, request);
        return map(incomeRepository.save(income));
    }

    public void delete(Long id, YearMonth effectiveMonth) {
        AppUser user = currentUserService.requireCurrentUser();
        Income income = incomeRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Receita nÃ£o encontrada."));

        if (!income.isRecurring()) {
            incomeRepository.delete(income);
            return;
        }

        LocalDate effectiveStart = effectiveMonth.atDay(1);
        if (!effectiveStart.isAfter(income.getReceiveDate())) {
            incomeRepository.delete(income);
            return;
        }

        income.setEndDate(effectiveStart.minusDays(1));
        incomeRepository.save(income);
    }

    private IncomeResponse updateRecurringIncome(Income income, IncomeRequest request) {
        Income updatedVersion = buildIncome(Income.builder().user(income.getUser()).build(), request);
        LocalDate effectiveStart = updatedVersion.getReceiveDate();
        LocalDate originalStart = income.getReceiveDate();

        if (!effectiveStart.isAfter(originalStart)) {
            income.setDescription(updatedVersion.getDescription());
            income.setAmount(updatedVersion.getAmount());
            income.setReceiveDate(updatedVersion.getReceiveDate());
            income.setExpectedDay(updatedVersion.getExpectedDay());
            income.setRecurring(true);
            return map(incomeRepository.save(income));
        }

        updatedVersion.setEndDate(income.getEndDate());
        income.setEndDate(effectiveStart.minusDays(1));
        incomeRepository.save(income);
        return map(incomeRepository.save(updatedVersion));
    }

    private Income buildIncome(Income income, IncomeRequest request) {
        if (request.recurring()) {
            Integer expectedDay = request.expectedDay();
            if (expectedDay == null && request.receiveDate() != null) {
                expectedDay = request.receiveDate().getDayOfMonth();
            }
            if (expectedDay == null) {
                throw new IllegalArgumentException("Informe o dia de recebimento para a receita fixa.");
            }

            YearMonth referenceMonth = request.receiveDate() != null
                    ? YearMonth.from(request.receiveDate())
                    : YearMonth.now();
            LocalDate referenceDate = referenceMonth.atDay(Math.min(expectedDay, referenceMonth.lengthOfMonth()));

            income.setDescription(request.description().trim());
            income.setAmount(request.amount());
            income.setReceiveDate(referenceDate);
            income.setEndDate(null);
            income.setExpectedDay(expectedDay);
            income.setRecurring(true);
            return income;
        }

        if (request.receiveDate() == null) {
            throw new IllegalArgumentException("Informe a data da receita mensal.");
        }

        income.setDescription(request.description().trim());
        income.setAmount(request.amount());
        income.setReceiveDate(request.receiveDate());
        income.setEndDate(null);
        income.setExpectedDay(request.receiveDate().getDayOfMonth());
        income.setRecurring(false);
        return income;
    }

    private boolean isRecurringActiveForMonth(Income income, YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        return !income.getReceiveDate().isAfter(monthEnd)
                && (income.getEndDate() == null || !income.getEndDate().isBefore(monthStart));
    }

    private IncomeResponse map(Income income) {
        return new IncomeResponse(
                income.getId(),
                income.getDescription(),
                income.getAmount(),
                income.getReceiveDate(),
                income.getExpectedDay(),
                income.isRecurring()
        );
    }
}
