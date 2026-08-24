package com.example.bankapp.exception;

import org.springframework.http.HttpStatus;

public class CardNotFoundException extends StoredValueException {

    public CardNotFoundException() {
        super("CARD_NOT_FOUND", HttpStatus.NOT_FOUND, "Stored-value card not found");
    }
}
