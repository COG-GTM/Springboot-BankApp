package com.example.bankapp.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InsufficientCardBalanceException extends StoredValueException {

    public InsufficientCardBalanceException(BigDecimal available) {
        super("INSUFFICIENT_BALANCE", HttpStatus.CONFLICT,
                "Redemption amount exceeds the available balance of " + available);
    }
}
