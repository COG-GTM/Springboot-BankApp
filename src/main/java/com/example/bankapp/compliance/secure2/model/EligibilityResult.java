package com.example.bankapp.compliance.secure2.model;

import java.math.BigDecimal;

public class EligibilityResult {

    private final boolean catchUpEligible;
    private final boolean highEarner;
    private final CatchUpTier catchUpTier;
    private final BigDecimal applicableCatchUpLimit;
    private final BigDecimal aggregatedFicaWages;
    private final BigDecimal ficaWageThreshold;
    private final boolean goodFaithDefault;

    private EligibilityResult(Builder builder) {
        this.catchUpEligible = builder.catchUpEligible;
        this.highEarner = builder.highEarner;
        this.catchUpTier = builder.catchUpTier;
        this.applicableCatchUpLimit = builder.applicableCatchUpLimit;
        this.aggregatedFicaWages = builder.aggregatedFicaWages;
        this.ficaWageThreshold = builder.ficaWageThreshold;
        this.goodFaithDefault = builder.goodFaithDefault;
    }

    public boolean isCatchUpEligible() {
        return catchUpEligible;
    }

    public boolean isHighEarner() {
        return highEarner;
    }

    public CatchUpTier getCatchUpTier() {
        return catchUpTier;
    }

    public BigDecimal getApplicableCatchUpLimit() {
        return applicableCatchUpLimit;
    }

    public BigDecimal getAggregatedFicaWages() {
        return aggregatedFicaWages;
    }

    public BigDecimal getFicaWageThreshold() {
        return ficaWageThreshold;
    }

    public boolean isGoodFaithDefault() {
        return goodFaithDefault;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean catchUpEligible;
        private boolean highEarner;
        private CatchUpTier catchUpTier = CatchUpTier.NONE;
        private BigDecimal applicableCatchUpLimit = BigDecimal.ZERO;
        private BigDecimal aggregatedFicaWages = BigDecimal.ZERO;
        private BigDecimal ficaWageThreshold = BigDecimal.ZERO;
        private boolean goodFaithDefault = false;

        public Builder catchUpEligible(boolean catchUpEligible) {
            this.catchUpEligible = catchUpEligible;
            return this;
        }

        public Builder highEarner(boolean highEarner) {
            this.highEarner = highEarner;
            return this;
        }

        public Builder catchUpTier(CatchUpTier catchUpTier) {
            this.catchUpTier = catchUpTier;
            return this;
        }

        public Builder applicableCatchUpLimit(BigDecimal applicableCatchUpLimit) {
            this.applicableCatchUpLimit = applicableCatchUpLimit;
            return this;
        }

        public Builder aggregatedFicaWages(BigDecimal aggregatedFicaWages) {
            this.aggregatedFicaWages = aggregatedFicaWages;
            return this;
        }

        public Builder ficaWageThreshold(BigDecimal ficaWageThreshold) {
            this.ficaWageThreshold = ficaWageThreshold;
            return this;
        }

        public Builder goodFaithDefault(boolean goodFaithDefault) {
            this.goodFaithDefault = goodFaithDefault;
            return this;
        }

        public EligibilityResult build() {
            return new EligibilityResult(this);
        }
    }
}
