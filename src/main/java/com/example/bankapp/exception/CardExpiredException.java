package com.example.bankapp.exception;

import org.springframework.http.HttpStatus;

public class CardExpiredException extends StoredValueException {

    public CardExpiredException() {
        super("CARD_EXPIRED", HttpStatus.CONFLICT, "Stored-value card has expired and cannot be redeemed");
    }
}
