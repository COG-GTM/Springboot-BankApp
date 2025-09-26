package com.example.bankapp.micronaut.dto;

import com.example.bankapp.model.Account;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class TransactionRequest {
    private Account account;

    public TransactionRequest() {}

    public TransactionRequest(Account account) {
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
