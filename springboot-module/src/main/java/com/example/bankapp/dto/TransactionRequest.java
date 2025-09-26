package com.example.bankapp.dto;

import com.example.bankapp.model.Account;

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
