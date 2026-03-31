package com.example.bankapp.compliance.secure2;

import com.example.bankapp.compliance.secure2.engine.ContributionLimitEngine;
import com.example.bankapp.compliance.secure2.model.CatchUpLimits;
import com.example.bankapp.compliance.secure2.model.CatchUpTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ContributionLimitEngineTest {

    private ContributionLimitEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ContributionLimitEngine();
    }

    @Test
    void defaultLimits2026() {
        CatchUpLimits limits = engine.getLimits(2026);
        assertEquals(new BigDecimal("8000"), limits.getStandardCatchUpLimit());
        assertEquals(new BigDecimal("150000"), limits.getFicaWageThreshold());
        // Super = greater of $10,000 or 150% of $8,000 = $12,000
        assertEquals(new BigDecimal("12000.0"), limits.getSuperCatchUpLimit());
    }

    @Test
    void defaultLimits2025() {
        CatchUpLimits limits = engine.getLimits(2025);
        assertEquals(new BigDecimal("7500"), limits.getStandardCatchUpLimit());
        assertEquals(new BigDecimal("145000"), limits.getFicaWageThreshold());
        // Super = greater of $10,000 or 150% of $7,500 = $11,250
        assertEquals(new BigDecimal("11250.0"), limits.getSuperCatchUpLimit());
    }

    @Test
    void superCatchUpLimit_floorOf10000() {
        // If standard catch-up were $6,000 => 150% = $9,000 < $10,000 floor
        engine.registerPlanYear(2030, new BigDecimal("6000"), new BigDecimal("160000"));
        CatchUpLimits limits = engine.getLimits(2030);
        assertEquals(new BigDecimal("10000"), limits.getSuperCatchUpLimit());
    }

    @Test
    void superCatchUpLimit_150percentWhenHigher() {
        // Standard $8,000 => 150% = $12,000 > $10,000
        CatchUpLimits limits = engine.getLimits(2026);
        assertEquals(new BigDecimal("12000.0"), limits.getSuperCatchUpLimit());
    }

    @Test
    void getCatchUpLimitForTier_standard() {
        assertEquals(new BigDecimal("8000"),
                engine.getCatchUpLimitForTier(CatchUpTier.STANDARD, 2026));
    }

    @Test
    void getCatchUpLimitForTier_super() {
        assertEquals(new BigDecimal("12000.0"),
                engine.getCatchUpLimitForTier(CatchUpTier.SUPER, 2026));
    }

    @Test
    void getCatchUpLimitForTier_standardPostSuper() {
        assertEquals(new BigDecimal("8000"),
                engine.getCatchUpLimitForTier(CatchUpTier.STANDARD_POST_SUPER, 2026));
    }

    @Test
    void getCatchUpLimitForTier_none() {
        assertEquals(BigDecimal.ZERO,
                engine.getCatchUpLimitForTier(CatchUpTier.NONE, 2026));
    }

    @Test
    void ficaWageThreshold2026() {
        assertEquals(new BigDecimal("150000"), engine.getFicaWageThreshold(2026));
    }

    @Test
    void registerCustomPlanYear() {
        engine.registerPlanYear(2028, new BigDecimal("8500"), new BigDecimal("155000"));
        CatchUpLimits limits = engine.getLimits(2028);
        assertEquals(new BigDecimal("8500"), limits.getStandardCatchUpLimit());
        assertEquals(new BigDecimal("155000"), limits.getFicaWageThreshold());
        // 150% of $8,500 = $12,750 > $10,000
        assertEquals(new BigDecimal("12750.0"), limits.getSuperCatchUpLimit());
    }

    @Test
    void projectedLimitsForFutureYear() {
        CatchUpLimits limits = engine.getLimits(2027);
        // Projected threshold = $150,000 + $5,000 * 1 = $155,000
        assertEquals(new BigDecimal("155000"), limits.getFicaWageThreshold());
    }
}
