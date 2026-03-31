package com.example.bankapp.compliance.secure2.engine;

import com.example.bankapp.compliance.secure2.model.CatchUpLimits;
import com.example.bankapp.compliance.secure2.model.CatchUpTier;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Engine for computing contribution limits per SECURE 2.0 §603 and §109.
 * Limits and thresholds are configurable per plan year to support annual IRS indexing.
 */
public class ContributionLimitEngine {

    private static final BigDecimal BASE_FICA_THRESHOLD = new BigDecimal("145000");
    private static final BigDecimal FICA_INDEX_INCREMENT = new BigDecimal("5000");

    private final Map<Integer, CatchUpLimits> limitsCache = new ConcurrentHashMap<>();

    public ContributionLimitEngine() {
        registerDefaults();
    }

    private void registerDefaults() {
        // 2026: $150,000 threshold (based on 2025 wages), $8,000 standard catch-up
        registerPlanYear(2026,
                new BigDecimal("8000"),
                new BigDecimal("150000"));

        // 2025 reference values (Section 109 super catch-up also effective for 2025)
        registerPlanYear(2025,
                new BigDecimal("7500"),
                new BigDecimal("145000"));
    }

    /**
     * Register or update limits for a specific plan year.
     * Super catch-up limit is computed as: greater of $10,000 or 150% of standard catch-up.
     */
    public void registerPlanYear(int planYear, BigDecimal standardCatchUpLimit,
                                 BigDecimal ficaWageThreshold) {
        BigDecimal superCatchUpLimit = computeSuperCatchUpLimit(standardCatchUpLimit);
        CatchUpLimits limits = new CatchUpLimits(planYear, standardCatchUpLimit,
                superCatchUpLimit, ficaWageThreshold);
        limitsCache.put(planYear, limits);
    }

    /**
     * Returns the catch-up limits for the given plan year.
     * If no limits have been explicitly registered, they are projected from the
     * base threshold using IRS indexing rules.
     */
    public CatchUpLimits getLimits(int planYear) {
        CatchUpLimits cached = limitsCache.get(planYear);
        if (cached != null) {
            return cached;
        }
        return projectLimitsForYear(planYear);
    }

    /**
     * Returns the applicable catch-up limit based on the participant's tier.
     */
    public BigDecimal getCatchUpLimitForTier(CatchUpTier tier, int planYear) {
        CatchUpLimits limits = getLimits(planYear);
        switch (tier) {
            case SUPER:
                return limits.getSuperCatchUpLimit();
            case STANDARD:
            case STANDARD_POST_SUPER:
                return limits.getStandardCatchUpLimit();
            case NONE:
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * Returns the FICA wage threshold for the given plan year.
     * Threshold comparison is "in excess of" (strictly greater than).
     */
    public BigDecimal getFicaWageThreshold(int planYear) {
        return getLimits(planYear).getFicaWageThreshold();
    }

    /**
     * Super catch-up limit per Section 109: the greater of $10,000 or
     * 150% of the standard catch-up contribution limit for the year.
     */
    private BigDecimal computeSuperCatchUpLimit(BigDecimal standardCatchUpLimit) {
        BigDecimal floor = new BigDecimal("10000");
        BigDecimal percentBased = standardCatchUpLimit
                .multiply(new BigDecimal("1.5"));
        return floor.max(percentBased);
    }

    /**
     * Projects limits for a future year using base indexing rules.
     * FICA threshold indexes in $5,000 increments from the $145,000 base.
     */
    private CatchUpLimits projectLimitsForYear(int planYear) {
        // Use 2026 as anchor: threshold = $150,000, standard = $8,000
        int yearsAfter2026 = planYear - 2026;
        BigDecimal projectedThreshold = new BigDecimal("150000")
                .add(FICA_INDEX_INCREMENT.multiply(BigDecimal.valueOf(Math.max(0, yearsAfter2026))));
        // Conservative: assume standard catch-up stays at $8,000 unless registered
        BigDecimal projectedStandard = new BigDecimal("8000");
        BigDecimal projectedSuper = computeSuperCatchUpLimit(projectedStandard);

        CatchUpLimits projected = new CatchUpLimits(planYear, projectedStandard,
                projectedSuper, projectedThreshold);
        limitsCache.put(planYear, projected);
        return projected;
    }
}
