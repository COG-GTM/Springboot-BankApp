package com.example.bankapp.exception;

public class AccountNotFoundException extends BankAppException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}
