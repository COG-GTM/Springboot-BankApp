package com.example.bankapp.dto;

import java.math.BigDecimal;

public class AccountRequest {
    private String username;
    private String password;
    private BigDecimal amount;
    private String toUsername;

    public AccountRequest() {}

    public AccountRequest(String username) {
        this.username = username;
    }

    public AccountRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public AccountRequest(String username, BigDecimal amount) {
        this.username = username;
        this.amount = amount;
    }

    public AccountRequest(String username, String toUsername, BigDecimal amount) {
        this.username = username;
        this.toUsername = toUsername;
        this.amount = amount;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getToUsername() {
        return toUsername;
    }

    public void setToUsername(String toUsername) {
        this.toUsername = toUsername;
    }
}
