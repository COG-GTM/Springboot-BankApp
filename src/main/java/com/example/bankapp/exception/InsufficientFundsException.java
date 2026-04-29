package com.example.bankapp.exception;

public class InsufficientFundsException extends BankAppException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
