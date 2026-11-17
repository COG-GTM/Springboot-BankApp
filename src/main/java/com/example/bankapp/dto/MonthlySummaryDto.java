package com.example.bankapp.dto;

import java.math.BigDecimal;

public class MonthlySummaryDto {
    private String month; // YYYY-MM
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal netBalance;

    public MonthlySummaryDto(String month, BigDecimal income, BigDecimal expenses, BigDecimal netBalance) {
        this.month = month;
        this.income = income;
        this.expenses = expenses;
        this.netBalance = netBalance;
    }

    public String getMonth() {
        return month;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public BigDecimal getExpenses() {
        return expenses;
    }

    public BigDecimal getNetBalance() {
        return netBalance;
    }
}
