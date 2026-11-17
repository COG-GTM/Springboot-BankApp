package com.example.bankapp.compliance.secure2.service;

import com.example.bankapp.compliance.secure2.engine.ContributionLimitEngine;
import com.example.bankapp.compliance.secure2.model.CatchUpLimits;
import com.example.bankapp.compliance.secure2.model.CatchUpTier;
import com.example.bankapp.compliance.secure2.model.EligibilityResult;
import com.example.bankapp.compliance.secure2.model.ParticipantInfo;
import com.example.bankapp.compliance.secure2.util.AgeCalculationUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * Determines catch-up contribution eligibility per SECURE 2.0 §603.
 *
 * <p>Key rules:
 * <ul>
 *   <li>Retrieves participant's prior-year FICA wages (Box 3, W-2) from payroll data</li>
 *   <li>Aggregates wages across controlled group employers per IRS final regs</li>
 *   <li>"In excess of" threshold means strictly greater than</li>
 *   <li>New hires with no prior-year W-2 default to non-high-earner (good-faith compliance)</li>
 * </ul>
 */
public class ContributionEligibilityService {

    private final ContributionLimitEngine limitEngine;

    public ContributionEligibilityService(ContributionLimitEngine limitEngine) {
        this.limitEngine = limitEngine;
    }

    /**
     * Evaluates catch-up contribution eligibility for a participant in the given plan year.
     */
    public EligibilityResult evaluate(ParticipantInfo participant, int planYear) {
        CatchUpTier tier = AgeCalculationUtil.determineCatchUpTier(
                participant.getDateOfBirth(), planYear);
        CatchUpLimits limits = limitEngine.getLimits(planYear);

        boolean catchUpEligible = (tier != CatchUpTier.NONE);
        BigDecimal applicableLimit = limitEngine.getCatchUpLimitForTier(tier, planYear);

        // New hires with no prior-year W-2: good-faith default to non-high-earner
        if (participant.isNewHire() || participant.getPriorYearFicaWages().isEmpty()) {
            return EligibilityResult.builder()
                    .catchUpEligible(catchUpEligible)
                    .highEarner(false)
                    .catchUpTier(tier)
                    .applicableCatchUpLimit(applicableLimit)
                    .aggregatedFicaWages(BigDecimal.ZERO)
                    .ficaWageThreshold(limits.getFicaWageThreshold())
                    .goodFaithDefault(true)
                    .build();
        }

        // Aggregate FICA wages across all controlled group employers
        BigDecimal aggregatedWages = aggregateFicaWages(participant.getPriorYearFicaWages());

        // "In excess of" means strictly greater than the threshold
        boolean highEarner = aggregatedWages.compareTo(limits.getFicaWageThreshold()) > 0;

        return EligibilityResult.builder()
                .catchUpEligible(catchUpEligible)
                .highEarner(highEarner)
                .catchUpTier(tier)
                .applicableCatchUpLimit(applicableLimit)
                .aggregatedFicaWages(aggregatedWages)
                .ficaWageThreshold(limits.getFicaWageThreshold())
                .goodFaithDefault(false)
                .build();
    }

    /**
     * Aggregates FICA wages across multiple employers in a controlled group.
     * Per IRS final regulations, wages from all employers in the controlled group
     * are combined for threshold determination.
     */
    BigDecimal aggregateFicaWages(List<BigDecimal> wagesList) {
        return wagesList.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
