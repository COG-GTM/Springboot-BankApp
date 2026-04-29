package com.example.bankapp.event;

import java.math.BigDecimal;

public class TransactionEvent {
    private final BigDecimal amount;
    private final String type;
    private final Long accountId;

    public TransactionEvent(BigDecimal amount, String type, Long accountId) {
        this.amount = amount;
        this.type = type;
        this.accountId = accountId;
    }

    public BigDecimal getAmount() { return amount; }
    public String getType() { return type; }
    public Long getAccountId() { return accountId; }
}
