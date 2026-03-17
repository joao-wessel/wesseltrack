package com.financeapp.backend.controller;

import com.financeapp.backend.dto.IncomeRequest;
import com.financeapp.backend.dto.IncomeResponse;
import com.financeapp.backend.service.IncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
public class IncomeController {

    private final IncomeService incomeService;

    @GetMapping
    public List<IncomeResponse> list(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return incomeService.list(month);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncomeResponse create(@Valid @RequestBody IncomeRequest request) {
        return incomeService.create(request);
    }

    @PutMapping("/{id}")
    public IncomeResponse update(@PathVariable Long id, @Valid @RequestBody IncomeRequest request) {
        return incomeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        incomeService.delete(id);
    }
}
