package com.example.bankapp.exception;

public abstract class BankAppException extends RuntimeException {

    protected BankAppException(String message) {
        super(message);
    }

    protected BankAppException(String message, Throwable cause) {
        super(message, cause);
    }
}
