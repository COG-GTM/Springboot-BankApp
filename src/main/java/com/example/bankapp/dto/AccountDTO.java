package com.example.bankapp.dto;

import java.math.BigDecimal;

public class AccountDTO {
    private Long id;
    private String username;
    private BigDecimal balance;

    public AccountDTO() {
    }

    public AccountDTO(Long id, String username, BigDecimal balance) {
        this.id = id;
        this.username = username;
        this.balance = balance;
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
