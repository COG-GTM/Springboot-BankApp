package com.example.bankapp.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionEvent {

    private final BigDecimal amount;
    private final String type;
    private final Long accountId;
    private final LocalDateTime timestamp;

    public TransactionEvent(BigDecimal amount, String type, Long accountId, LocalDateTime timestamp) {
        this.amount = amount;
        this.type = type;
        this.accountId = accountId;
        this.timestamp = timestamp;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public Long getAccountId() {
        return accountId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
