package com.example.bankapp.micronaut.dto;

import com.example.bankapp.model.Account;
import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;

@Serdeable
public class TransferRequest {
    private Account fromAccount;
    private String toUsername;
    private BigDecimal amount;

    public TransferRequest() {}

    public TransferRequest(Account fromAccount, String toUsername, BigDecimal amount) {
        this.fromAccount = fromAccount;
        this.toUsername = toUsername;
        this.amount = amount;
    }

    public Account getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(Account fromAccount) {
        this.fromAccount = fromAccount;
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
