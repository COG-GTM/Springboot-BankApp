package com.example.bankapp.dto;

import java.math.BigDecimal;

public class TransferRequest {
    private String toUsername;
    private BigDecimal amount;

    public TransferRequest() {
    }

    public TransferRequest(String toUsername, BigDecimal amount) {
        this.toUsername = toUsername;
        this.amount = amount;
    }

    public String getToUsername() {
        return toUsername;
    }

    public void setToUsername(String toUsername) {
        this.toUsername = toUsername;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
