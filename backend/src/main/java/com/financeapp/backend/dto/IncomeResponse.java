package com.financeapp.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeResponse(Long id, String description, BigDecimal amount, LocalDate receiveDate, Integer expectedDay) {
}
