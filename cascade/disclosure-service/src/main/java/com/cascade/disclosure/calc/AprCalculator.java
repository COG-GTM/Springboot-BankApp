package com.cascade.disclosure.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regulation Z, 12 CFR §1026.22(a)(1): the APR is the rate that yields the amount financed when the stream of
 * payments is discounted by the actuarial method (Appendix J). Equal monthly payments, unit period = month.
 * §1026.22(a)(2): accurate for a regular transaction if within 1/8 of 1 percentage point of this figure.
 */
public final class AprCalculator {
    private AprCalculator() {}

    /** Annual percentage rate (percent, 2dp) for {@code n} equal monthly payments on {@code amountFinanced}. */
    public static BigDecimal actuarialApr(BigDecimal amountFinanced, BigDecimal payment, int n) {
        double af = amountFinanced.doubleValue();
        double pmt = payment.doubleValue();
        double lo = 0.0;
        double hi = 1.0;
        for (int i = 0; i < 200; i++) {
            double mid = (lo + hi) / 2;
            double pv = mid > 0 ? pmt * (1 - Math.pow(1 + mid, -n)) / mid : pmt * n;
            if (pv > af) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return BigDecimal.valueOf(1200.0 * (lo + hi) / 2).setScale(2, RoundingMode.HALF_UP);
    }

    /** Level monthly payment for a nominal annual rate (used to reproduce the core's payment schedule). */
    public static BigDecimal levelPayment(BigDecimal principal, BigDecimal nominalRate, int n) {
        double i = nominalRate.doubleValue() / 12;
        double p = principal.doubleValue();
        double pmt = i > 0 ? p * i / (1 - Math.pow(1 + i, -n)) : p / n;
        return BigDecimal.valueOf(pmt).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean withinTolerance(BigDecimal disclosed, BigDecimal actuarial, BigDecimal tolerancePp) {
        return disclosed.subtract(actuarial).abs().compareTo(tolerancePp) <= 0;
    }
}
