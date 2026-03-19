package com.financeapp.backend.controller;

import com.financeapp.backend.dto.MonthlyPlanningResponse;
import com.financeapp.backend.dto.PlanningSettingsRequest;
import com.financeapp.backend.dto.PlanningSettingsResponse;
import com.financeapp.backend.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @GetMapping("/planning")
    public MonthlyPlanningResponse getPlanning(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return goalService.getPlanning(month);
    }

    @GetMapping("/settings")
    public PlanningSettingsResponse getSettings() {
        return goalService.getSettings();
    }

    @PutMapping("/settings")
    public PlanningSettingsResponse saveSettings(@Valid @RequestBody PlanningSettingsRequest request) {
        return goalService.saveSettings(request);
    }
}
