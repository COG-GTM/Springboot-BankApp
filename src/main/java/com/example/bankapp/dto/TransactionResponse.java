package com.example.bankapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private BigDecimal amount;
    private String type;
    private LocalDateTime timestamp;

    public TransactionResponse(Long id, BigDecimal amount, String type, LocalDateTime timestamp) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
