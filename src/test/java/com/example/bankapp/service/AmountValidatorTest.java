package com.example.bankapp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class AmountValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "1", "100", "100.5", "100.50", "1E+2", "999999999999.99", "1000000000000", "1E+12"})
    void acceptsPositiveAmountsWithinLimits(String amount) {
        assertDoesNotThrow(() -> AmountValidator.requireValid(new BigDecimal(amount)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0", "0.00", "-1", "-0.01",
            "0.001", "1.005",
            "1000000000000.01", "1E+13", "9999999999999",
            "1E+500000000", "-1E+500000000", "1E-500000000",
            "1E+2147483647", "-1E+2147483647", "1E-2147483647"
    })
    void rejectsNonPositiveOverPreciseOrHugeAmountsWithoutArithmetic(String amount) {
        BigDecimal value = new BigDecimal(amount);
        assertTimeoutPreemptively(Duration.ofMillis(500), () ->
                assertThrows(InvalidAmountException.class, () -> AmountValidator.requireValid(value)));
    }

    @Test
    void rejectsNull() {
        assertThrows(InvalidAmountException.class, () -> AmountValidator.requireValid(null));
    }
}
