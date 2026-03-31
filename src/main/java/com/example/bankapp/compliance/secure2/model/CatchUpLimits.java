package com.example.bankapp.compliance.secure2.model;

import java.math.BigDecimal;

public class CatchUpLimits {

    private final int planYear;
    private final BigDecimal standardCatchUpLimit;
    private final BigDecimal superCatchUpLimit;
    private final BigDecimal ficaWageThreshold;

    public CatchUpLimits(int planYear, BigDecimal standardCatchUpLimit,
                         BigDecimal superCatchUpLimit, BigDecimal ficaWageThreshold) {
        this.planYear = planYear;
        this.standardCatchUpLimit = standardCatchUpLimit;
        this.superCatchUpLimit = superCatchUpLimit;
        this.ficaWageThreshold = ficaWageThreshold;
    }

    public int getPlanYear() {
        return planYear;
    }

    public BigDecimal getStandardCatchUpLimit() {
        return standardCatchUpLimit;
    }

    public BigDecimal getSuperCatchUpLimit() {
        return superCatchUpLimit;
    }

    public BigDecimal getFicaWageThreshold() {
        return ficaWageThreshold;
    }
}
