package com.cascade.disclosure.calc;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ApyCalculatorTest {

    @Test
    void appendixAPartIExampleNowAccount() {
        // Appendix A Part I: $61.68 interest on $1,000 for a 365-day year -> APY 6.17%
        assertThat(ApyCalculator.apy(new BigDecimal("61.68"), new BigDecimal("1000.00"), 365)).isEqualByComparingTo("6.17");
    }

    @Test
    void dailyCompoundingOfStatedRateMatchesProductFixture() {
        assertThat(ApyCalculator.apyFromNominalRate(new BigDecimal("0.036500"), 365)).isEqualByComparingTo("3.72");
        assertThat(ApyCalculator.apyFromNominalRate(new BigDecimal("0.045000"), 365)).isEqualByComparingTo("4.60");
        assertThat(ApyCalculator.apyFromNominalRate(new BigDecimal("0.052000"), 365)).isEqualByComparingTo("5.34");
        assertThat(ApyCalculator.apyFromNominalRate(BigDecimal.ZERO, 365)).isEqualByComparingTo("0.00");
    }

    @Test
    void roundsToNearestHundredthOfAPercentagePoint() {
        assertThat(ApyCalculator.roundPercentagePoints(new BigDecimal("5.1267"))).isEqualTo(new BigDecimal("5.13"));
        assertThat(ApyCalculator.roundPercentagePoints(new BigDecimal("5.125"))).isEqualTo(new BigDecimal("5.13"));
    }

    @Test
    void toleranceIsFiveHundredthsOfAPercentagePoint() {
        BigDecimal tol = new BigDecimal("0.05");
        assertThat(ApyCalculator.withinTolerance(new BigDecimal("3.72"), new BigDecimal("3.77"), tol)).isTrue();
        assertThat(ApyCalculator.withinTolerance(new BigDecimal("3.72"), new BigDecimal("3.78"), tol)).isFalse();
    }

    @Test
    void oneCentADayOnASmallBalanceIsOutsideTolerance() {
        // 1,050.00 balance over 90 days: 9.76 vs 9.45 of statement cents -> APY earned differs by more than 0.05 pp
        BigDecimal bal = new BigDecimal("1050.00");
        BigDecimal legacy = ApyCalculator.apy(new BigDecimal("9.76"), bal, 90);
        BigDecimal migrated = ApyCalculator.apy(new BigDecimal("9.45"), bal, 90);
        assertThat(legacy.subtract(migrated).abs()).isGreaterThan(new BigDecimal("0.05"));
    }
}
