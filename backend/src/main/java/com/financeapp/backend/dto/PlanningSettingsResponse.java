package com.financeapp.backend.dto;

import java.math.BigDecimal;

public record PlanningSettingsResponse(
        BigDecimal reserveGoal,
        BigDecimal creditLimit,
        Integer creditCardClosingDay
) {
}
