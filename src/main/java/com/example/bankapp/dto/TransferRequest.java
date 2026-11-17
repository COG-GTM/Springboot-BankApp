package com.example.bankapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class TransferRequest {

    @NotBlank(message = "Recipient username is required")
    @Pattern(regexp = "^[\\w.-]+$", message = "Recipient username can only contain alphanumeric characters, underscores, dots, and hyphens")
    private String toUsername;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
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
