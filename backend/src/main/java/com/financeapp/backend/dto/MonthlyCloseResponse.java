package com.financeapp.backend.dto;

public record MonthlyCloseResponse(String sourceMonth, String targetMonth, int createdExpenses) {
}
