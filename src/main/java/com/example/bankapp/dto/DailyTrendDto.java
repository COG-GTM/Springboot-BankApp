package com.example.bankapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyTrendDto {
    private LocalDate date;
    private BigDecimal totalSpending;

    public DailyTrendDto(LocalDate date, BigDecimal totalSpending) {
        this.date = date;
        this.totalSpending = totalSpending;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getTotalSpending() {
        return totalSpending;
    }
}
