package com.financeapp.backend.service;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.Income;
import com.financeapp.backend.dto.IncomeRequest;
import com.financeapp.backend.dto.IncomeResponse;
import com.financeapp.backend.repository.IncomeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final CurrentUserService currentUserService;

    public List<IncomeResponse> list(YearMonth month) {
        AppUser user = currentUserService.requireCurrentUser();
        return incomeRepository.findAllByUserAndReceiveDateBetweenOrderByReceiveDateAsc(user, month.atDay(1), month.atEndOfMonth()).stream()
                .map(this::map)
                .toList();
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
        income.setDescription(request.description().trim());
        income.setAmount(request.amount());
        income.setReceiveDate(request.receiveDate());
        return income;
    }

    private IncomeResponse map(Income income) {
        return new IncomeResponse(income.getId(), income.getDescription(), income.getAmount(), income.getReceiveDate());
    }
}
