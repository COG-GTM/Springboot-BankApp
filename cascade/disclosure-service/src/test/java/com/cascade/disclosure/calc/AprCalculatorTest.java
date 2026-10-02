package com.cascade.disclosure.calc;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class AprCalculatorTest {

    @Test
    void levelPaymentReproducesCoreSchedule() {
        assertThat(AprCalculator.levelPayment(new BigDecimal("25000.00"), new BigDecimal("0.0749"), 60)).isEqualByComparingTo("500.83");
        assertThat(AprCalculator.levelPayment(new BigDecimal("10000.00"), new BigDecimal("0.1199"), 36)).isEqualByComparingTo("332.10");
    }

    @Test
    void aprEqualsNominalRateWithoutPrepaidFinanceCharges() {
        assertThat(AprCalculator.actuarialApr(new BigDecimal("25000.00"), new BigDecimal("500.83"), 60)).isEqualByComparingTo("7.49");
    }

    @Test
    void prepaidFinanceChargeRaisesAprAboveNominal() {
        BigDecimal apr = AprCalculator.actuarialApr(new BigDecimal("9850.00"), new BigDecimal("332.10"), 36);
        assertThat(apr).isEqualByComparingTo("13.05");
        assertThat(apr).isGreaterThan(new BigDecimal("11.99"));
    }

    @Test
    void toleranceIsOneEighthOfAPercentagePoint() {
        BigDecimal tol = new BigDecimal("0.125");
        assertThat(AprCalculator.withinTolerance(new BigDecimal("7.49"), new BigDecimal("7.61"), tol)).isTrue();
        assertThat(AprCalculator.withinTolerance(new BigDecimal("7.49"), new BigDecimal("7.62"), tol)).isFalse();
    }
}
