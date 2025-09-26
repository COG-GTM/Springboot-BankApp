package com.example.bankapp.micronaut.dto;

import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;

@Serdeable
public class DepositWithdrawRequest {
    private String username;
    private BigDecimal amount;

    public DepositWithdrawRequest() {}

    public DepositWithdrawRequest(String username, BigDecimal amount) {
        this.username = username;
        this.amount = amount;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
