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
                .orElseThrow(() -> new EntityNotFoundException("Receita não encontrada."));
        income = buildIncome(income, request);
        return map(incomeRepository.save(income));
    }

    public void delete(Long id) {
        AppUser user = currentUserService.requireCurrentUser();
        Income income = incomeRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Receita não encontrada."));
        incomeRepository.delete(income);
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
        income.setExpectedDay(request.receiveDate().getDayOfMonth());
        income.setRecurring(false);
        return income;
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