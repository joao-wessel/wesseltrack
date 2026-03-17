package com.financeapp.backend.controller;

import com.financeapp.backend.dto.MonthlyCloseResponse;
import com.financeapp.backend.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/monthly-close")
@RequiredArgsConstructor
public class MonthlyCloseController {

    private final ExpenseService expenseService;

    @PostMapping
    public MonthlyCloseResponse closeMonth(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        int createdExpenses = expenseService.closeMonth(month);
        return new MonthlyCloseResponse(month.toString(), month.plusMonths(1).toString(), createdExpenses);
    }
}
