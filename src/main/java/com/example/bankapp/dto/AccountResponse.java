package com.example.bankapp.dto;

import com.example.bankapp.model.Account;

import java.math.BigDecimal;

public class AccountResponse {

    private Long id;
    private String username;
    private BigDecimal balance;

    public AccountResponse() {
    }

    public AccountResponse(Long id, String username, BigDecimal balance) {
        this.id = id;
        this.username = username;
        this.balance = balance;
    }

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getUsername(), account.getBalance());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
