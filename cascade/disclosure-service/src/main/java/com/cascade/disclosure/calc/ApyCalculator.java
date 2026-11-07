package com.cascade.disclosure.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Regulation DD, 12 CFR Part 1030, Appendix A.
 * Part I (general formula): APY = 100 [(1 + Interest/Principal)^(365/Days in term) − 1].
 * Part II (periodic statements): APY Earned = 100 [(1 + Interest/Balance)^(365/Days in period) − 1].
 * Results are rounded to the nearest one-hundredth of one percentage point (§1030.3(f)(1)).
 */
public final class ApyCalculator {
    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int YEAR_DAYS = 365;

    private ApyCalculator() {}

    /** Appendix A Part I / Part II general formula. */
    public static BigDecimal apy(BigDecimal interest, BigDecimal principal, int days) {
        if (principal.signum() <= 0 || days <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        double base = 1.0 + interest.divide(principal, MC).doubleValue();
        double apy = 100.0 * (Math.pow(base, (double) YEAR_DAYS / days) - 1.0);
        return roundPercentagePoints(BigDecimal.valueOf(apy));
    }

    /** Disclosed APY for a nominal rate compounded {@code periodsPerYear} times over a one-year term. */
    public static BigDecimal apyFromNominalRate(BigDecimal nominalRate, int periodsPerYear) {
        if (nominalRate.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        double r = nominalRate.doubleValue() / periodsPerYear;
        double apy = 100.0 * (Math.pow(1.0 + r, periodsPerYear) - 1.0);
        return roundPercentagePoints(BigDecimal.valueOf(apy));
    }

    /** §1030.3(f)(1): nearest one-hundredth of one percentage point, two decimal places. */
    public static BigDecimal roundPercentagePoints(BigDecimal pp) {
        return pp.setScale(2, RoundingMode.HALF_UP);
    }

    /** §1030.3(f)(2): accurate if not more than 0.05 percentage point above or below the Appendix A figure. */
    public static boolean withinTolerance(BigDecimal disclosed, BigDecimal appendixA, BigDecimal tolerancePp) {
        return disclosed.subtract(appendixA).abs().compareTo(tolerancePp) <= 0;
    }

    public static BigDecimal hundred() {
        return HUNDRED;
    }
}
