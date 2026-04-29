package com.example.bankapp.exception;

public class InvalidAmountException extends BankAppException {

    public InvalidAmountException(String message) {
        super(message);
    }
}
