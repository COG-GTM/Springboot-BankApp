package com.example.bankapp.micronaut.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class TransactionRequest {
    private String username;

    public TransactionRequest() {}

    public TransactionRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
