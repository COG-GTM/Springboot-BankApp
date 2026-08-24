package com.example.bankapp.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyKeyConflictException extends StoredValueException {

    public IdempotencyKeyConflictException() {
        super("IDEMPOTENCY_KEY_CONFLICT", HttpStatus.CONFLICT,
                "Idempotency-Key was already used on this card with a different redemption amount");
    }
}
