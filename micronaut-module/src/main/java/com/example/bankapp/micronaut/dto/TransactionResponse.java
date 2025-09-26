package com.example.bankapp.micronaut.dto;

import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Serdeable
public class TransactionResponse {
    private Long id;
    private BigDecimal amount;
    private String description;
    private LocalDateTime timestamp;
    private String accountUsername;

    public TransactionResponse() {}

    public TransactionResponse(Long id, BigDecimal amount, String description, LocalDateTime timestamp, String accountUsername) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
        this.accountUsername = accountUsername;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAccountUsername() {
        return accountUsername;
    }

    public void setAccountUsername(String accountUsername) {
        this.accountUsername = accountUsername;
    }
}
