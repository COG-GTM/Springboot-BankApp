package com.example.bankapp.micronaut.dto;

import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;

@Serdeable
public class TransferRequest {
    private String fromUsername;
    private String toUsername;
    private BigDecimal amount;

    public TransferRequest() {}

    public TransferRequest(String fromUsername, String toUsername, BigDecimal amount) {
        this.fromUsername = fromUsername;
        this.toUsername = toUsername;
        this.amount = amount;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
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
