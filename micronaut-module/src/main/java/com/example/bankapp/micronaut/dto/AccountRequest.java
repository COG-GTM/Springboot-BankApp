package com.example.bankapp.micronaut.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class AccountRequest {
    private String username;
    private String password;

    public AccountRequest() {}

    public AccountRequest(String username) {
        this.username = username;
    }

    public AccountRequest(String username, String password) {
        this.username = username;
        this.password = password;
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
}
