package com.example.bankapp.exception;

public class DuplicateUsernameException extends BankAppException {

    public DuplicateUsernameException(String message) {
        super(message);
    }
}
