package com.example.bankapp.compliance.secure2.engine;

import com.example.bankapp.compliance.secure2.model.ContributionDesignation;
import com.example.bankapp.compliance.secure2.model.ContributionRequest;
import com.example.bankapp.compliance.secure2.model.ContributionType;
import com.example.bankapp.compliance.secure2.model.EligibilityResult;
import com.example.bankapp.compliance.secure2.model.PlanType;
import com.example.bankapp.compliance.secure2.model.ParticipantInfo;
import com.example.bankapp.compliance.secure2.model.RoutingResult;
import com.example.bankapp.compliance.secure2.service.ContributionEligibilityService;
import com.example.bankapp.compliance.secure2.util.AgeCalculationUtil;

/**
 * Routes contributions based on SECURE 2.0 §603 rules.
 *
 * <p>Routing logic:
 * <ul>
 *   <li>Regular (non-catch-up) contributions are unaffected regardless of income</li>
 *   <li>Catch-up contributions for high earners (prior-year FICA wages &gt; threshold) must be Roth</li>
 *   <li>Pre-tax catch-up attempt for an affected participant is rejected with ROTH_CATCHUP_REQUIRED</li>
 *   <li>Super catch-up (Section 109, ages 60–63) for high earners must also be Roth</li>
 *   <li>Governmental 457(b) special catch-up (3 years before NRA) is exempt from Roth requirement</li>
 * </ul>
 */
public class ContributionRoutingEngine {

    private final ContributionEligibilityService eligibilityService;

    public ContributionRoutingEngine(ContributionEligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    /**
     * Evaluates a contribution request and returns routing result.
     */
    public RoutingResult route(ContributionRequest request) {
        // Regular contributions are never affected by §603
        if (request.getContributionType() == ContributionType.REGULAR) {
            return RoutingResult.accepted(
                    request.getDesignation(),
                    "Regular contribution accepted; §603 does not apply");
        }

        ParticipantInfo participant = request.getParticipantInfo();
        int planYear = request.getPlanYear();

        // Check catch-up eligibility (age 50+)
        if (!AgeCalculationUtil.isCatchUpEligible(participant.getDateOfBirth(), planYear)) {
            return RoutingResult.rejected(
                    RoutingResult.ERROR_NOT_CATCHUP_ELIGIBLE,
                    "Participant is not yet age 50 in plan year " + planYear
                            + "; not eligible for catch-up contributions");
        }

        // Evaluate income-based eligibility
        EligibilityResult eligibility = eligibilityService.evaluate(participant, planYear);

        // Check governmental 457(b) special catch-up exemption
        if (isGovernmental457bSpecialCatchUpExempt(participant, planYear)) {
            return RoutingResult.accepted(
                    request.getDesignation(),
                    "Governmental 457(b) special catch-up is exempt from §603 Roth requirement");
        }

        // If participant is a high earner, catch-up must be Roth
        if (eligibility.isHighEarner()) {
            if (request.getDesignation() == ContributionDesignation.PRE_TAX) {
                return RoutingResult.rejected(
                        RoutingResult.ERROR_ROTH_CATCHUP_REQUIRED,
                        "Participant's prior-year FICA wages exceed $"
                                + eligibility.getFicaWageThreshold().toPlainString()
                                + "; catch-up contributions must be designated Roth per §603");
            }
            // Roth designation for high earner — accepted
            return RoutingResult.accepted(
                    ContributionDesignation.ROTH,
                    "High earner catch-up contribution accepted as Roth per §603");
        }

        // Not a high earner — participant can choose pre-tax or Roth
        return RoutingResult.accepted(
                request.getDesignation(),
                "Catch-up contribution accepted; participant is below FICA wage threshold");
    }

    /**
     * Determines if a contribution qualifies for the governmental 457(b) special catch-up
     * exemption. The special catch-up applies during the 3 plan years ending before the
     * participant reaches normal retirement age (NRA). This special catch-up is exempt
     * from the §603 Roth requirement; only amounts exceeding the special catch-up limit
     * must be Roth-designated.
     */
    private boolean isGovernmental457bSpecialCatchUpExempt(ParticipantInfo participant,
                                                           int planYear) {
        if (participant.getPlanType() != PlanType.GOVERNMENTAL_457B) {
            return false;
        }

        Integer nra = participant.getNormalRetirementAge();
        if (nra == null) {
            return false;
        }

        int ageInYear = AgeCalculationUtil.ageAttainedInYear(
                participant.getDateOfBirth(), planYear);

        // Special catch-up window: 3 plan years ending before NRA
        // i.e., ages NRA-3, NRA-2, NRA-1
        int windowStart = nra - 3;
        return ageInYear >= windowStart && ageInYear < nra;
    }
}
