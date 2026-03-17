package com.financeapp.backend.controller;

import com.financeapp.backend.dto.MonthlyGoalRequest;
import com.financeapp.backend.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public BigDecimal save(@Valid @RequestBody MonthlyGoalRequest request) {
        return goalService.save(request);
    }
}
