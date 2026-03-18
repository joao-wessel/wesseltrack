package com.financeapp.backend.controller;

import com.financeapp.backend.dto.MonthlyGoalRequest;
import com.financeapp.backend.dto.MonthlyPlanningRequest;
import com.financeapp.backend.dto.MonthlyPlanningResponse;
import com.financeapp.backend.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public BigDecimal save(@Valid @RequestBody MonthlyGoalRequest request) {
        return goalService.save(request);
    }

    @GetMapping("/planning")
    public MonthlyPlanningResponse getPlanning(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return goalService.getPlanning(month);
    }

    @PostMapping("/planning")
    public MonthlyPlanningResponse savePlanning(@Valid @RequestBody MonthlyPlanningRequest request) {
        return goalService.savePlanning(request);
    }
}
