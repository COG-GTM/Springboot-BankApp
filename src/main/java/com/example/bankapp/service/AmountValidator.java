package com.example.bankapp.service;

import java.math.BigDecimal;

/**
 * Rejects amounts before any arithmetic is done on them. Only scale(), precision(),
 * signum() and compareTo() are used, so a value such as 1E+500000000 is rejected without
 * BigDecimal ever rescaling it to a common scale with the balance.
 */
public final class AmountValidator {

    public static final int MAX_SCALE = 2;
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");
    private static final long MAX_INTEGER_DIGITS = MAX_AMOUNT.precision() - MAX_AMOUNT.scale();

    private AmountValidator() {
    }

    public static void requireValid(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("Amount is required");
        }
        if (amount.scale() > MAX_SCALE) {
            throw new InvalidAmountException("Amount must have at most " + MAX_SCALE + " decimal places");
        }
        if (amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        // long arithmetic: precision - scale overflows int for exponents near Integer.MAX_VALUE
        long integerDigits = (long) amount.precision() - amount.scale();
        if (integerDigits > MAX_INTEGER_DIGITS || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidAmountException("Amount must not exceed " + MAX_AMOUNT.toPlainString());
        }
    }
}
