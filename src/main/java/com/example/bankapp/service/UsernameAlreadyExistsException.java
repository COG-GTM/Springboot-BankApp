package com.example.bankapp.service;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException() {
        super("User already present.");
    }
}
