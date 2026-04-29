package com.example.bankapp.dto;

import java.math.BigDecimal;

public class WithdrawRequest {

    private BigDecimal amount;

    public WithdrawRequest() {
    }

    public WithdrawRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
