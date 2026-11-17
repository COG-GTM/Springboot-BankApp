package com.example.bankapp.dto;

import java.math.BigDecimal;

public class CategorySpendingDto {
    private String category;
    private BigDecimal total;
    private long transactionCount;

    public CategorySpendingDto(String category, BigDecimal total, long transactionCount) {
        this.category = category;
        this.total = total;
        this.transactionCount = transactionCount;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public long getTransactionCount() {
        return transactionCount;
    }
}
