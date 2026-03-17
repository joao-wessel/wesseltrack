package com.financeapp.backend.controller;

import com.financeapp.backend.dto.DashboardSummaryResponse;
import com.financeapp.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/monthly")
    public DashboardSummaryResponse monthly(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return dashboardService.getMonthlySummary(month);
    }
}
