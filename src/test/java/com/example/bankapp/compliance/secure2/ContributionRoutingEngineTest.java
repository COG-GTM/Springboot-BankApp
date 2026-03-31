package com.example.bankapp.compliance.secure2;

import com.example.bankapp.compliance.secure2.engine.ContributionLimitEngine;
import com.example.bankapp.compliance.secure2.engine.ContributionRoutingEngine;
import com.example.bankapp.compliance.secure2.model.*;
import com.example.bankapp.compliance.secure2.service.ContributionEligibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ContributionRoutingEngineTest {

    private ContributionRoutingEngine routingEngine;

    @BeforeEach
    void setUp() {
        ContributionLimitEngine limitEngine = new ContributionLimitEngine();
        ContributionEligibilityService eligibilityService =
                new ContributionEligibilityService(limitEngine);
        routingEngine = new ContributionRoutingEngine(eligibilityService);
    }

    private ParticipantInfo participant(int birthYear, int birthMonth, BigDecimal ficaWages) {
        return ParticipantInfo.builder()
                .participantId("TEST")
                .dateOfBirth(LocalDate.of(birthYear, birthMonth, 1))
                .priorYearFicaWages(List.of(ficaWages))
                .build();
    }

    private ParticipantInfo participantNewHire(int birthYear, int birthMonth) {
        return ParticipantInfo.builder()
                .participantId("TEST")
                .dateOfBirth(LocalDate.of(birthYear, birthMonth, 1))
                .newHire(true)
                .priorYearFicaWages(Collections.emptyList())
                .build();
    }

    private ParticipantInfo participantWithControlledGroup(int birthYear, int birthMonth,
                                                           BigDecimal... wages) {
        return ParticipantInfo.builder()
                .participantId("TEST")
                .dateOfBirth(LocalDate.of(birthYear, birthMonth, 1))
                .priorYearFicaWages(Arrays.asList(wages))
                .build();
    }

    private ParticipantInfo participant457b(int birthYear, int birthMonth,
                                            BigDecimal ficaWages, int normalRetirementAge) {
        return ParticipantInfo.builder()
                .participantId("TEST")
                .dateOfBirth(LocalDate.of(birthYear, birthMonth, 1))
                .priorYearFicaWages(List.of(ficaWages))
                .planType(PlanType.GOVERNMENTAL_457B)
                .normalRetirementAge(normalRetirementAge)
                .build();
    }

    // =========================================================================
    // Core Routing — Scenarios 1–6
    // =========================================================================

    @Nested
    @DisplayName("Core Routing Scenarios")
    class CoreRouting {

        @Test
        @DisplayName("Scenario 1: Age 52, $160K wages, pre-tax catch-up => REJECTED")
        void scenario1_highEarner_preTaxCatchUp_rejected() {
            ParticipantInfo p = participant(1974, 6, new BigDecimal("160000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertFalse(result.isAccepted());
            assertEquals(RoutingResult.ERROR_ROTH_CATCHUP_REQUIRED, result.getErrorCode());
        }

        @Test
        @DisplayName("Scenario 2: Age 52, $160K wages, Roth catch-up => ACCEPTED")
        void scenario2_highEarner_rothCatchUp_accepted() {
            ParticipantInfo p = participant(1974, 6, new BigDecimal("160000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.ROTH)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
            assertEquals(ContributionDesignation.ROTH, result.getEffectiveDesignation());
        }

        @Test
        @DisplayName("Scenario 3: Age 52, $130K wages, pre-tax catch-up => ACCEPTED")
        void scenario3_belowThreshold_preTaxCatchUp_accepted() {
            ParticipantInfo p = participant(1974, 6, new BigDecimal("130000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
            assertEquals(ContributionDesignation.PRE_TAX, result.getEffectiveDesignation());
        }

        @Test
        @DisplayName("Scenario 4: Age 52, $130K wages, Roth catch-up => ACCEPTED")
        void scenario4_belowThreshold_rothCatchUp_accepted() {
            ParticipantInfo p = participant(1974, 6, new BigDecimal("130000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.ROTH)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
            assertEquals(ContributionDesignation.ROTH, result.getEffectiveDesignation());
        }

        @Test
        @DisplayName("Scenario 5: Age 48, $200K wages, regular contribution => ACCEPTED (rule doesn't apply)")
        void scenario5_under50_regularContribution_accepted() {
            ParticipantInfo p = participant(1978, 5, new BigDecimal("200000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.REGULAR)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("23000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
            assertEquals(ContributionDesignation.PRE_TAX, result.getEffectiveDesignation());
        }

        @Test
        @DisplayName("Scenario 6: Age 52, exactly $145K wages (2025 base), pre-tax catch-up => ACCEPTED (threshold is 'in excess of')")
        void scenario6_exactlyAtBaseThreshold_preTaxCatchUp_accepted() {
            // Ticket says $145,000 for scenario 6.
            // For 2026, threshold is $150,000. At exactly $145,000, clearly below => accepted.
            // This tests the "in excess of" interpretation.
            ParticipantInfo p = participant(1974, 6, new BigDecimal("145000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
        }

        @Test
        @DisplayName("Scenario 6b: Exactly at $150K threshold for 2026, pre-tax catch-up => ACCEPTED ('in excess of' = strictly greater)")
        void scenario6b_exactlyAt150kThreshold_accepted() {
            ParticipantInfo p = participant(1974, 6, new BigDecimal("150000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted(), "Exactly at threshold should be accepted — 'in excess of' means strictly greater");
        }
    }

    // =========================================================================
    // Super Catch-Up Interaction — Scenarios 7–10
    // =========================================================================

    @Nested
    @DisplayName("Super Catch-Up Interaction Scenarios")
    class SuperCatchUp {

        @Test
        @DisplayName("Scenario 7: Age 61, $160K wages, $11,250 super catch-up => ACCEPTED, Roth only")
        void scenario7_superCatchUp_highEarner_rothOnly() {
            ParticipantInfo p = participant(1965, 7, new BigDecimal("160000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.SUPER_CATCH_UP)
                    .designation(ContributionDesignation.ROTH)
                    .amount(new BigDecimal("11250"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
            assertEquals(ContributionDesignation.ROTH, result.getEffectiveDesignation());
        }

        @Test
        @DisplayName("Scenario 7b: Age 61, $160K wages, super catch-up PRE_TAX => REJECTED")
        void scenario7b_superCatchUp_highEarner_preTax_rejected() {
            ParticipantInfo p = participant(1965, 7, new BigDecimal("160000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.SUPER_CATCH_UP)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("11250"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertFalse(result.isAccepted());
            assertEquals(RoutingResult.ERROR_ROTH_CATCHUP_REQUIRED, result.getErrorCode());
        }

        @Test
        @DisplayName("Scenario 8: Age 61, $130K wages, $11,250 super catch-up => ACCEPTED, pre-tax or Roth")
        void scenario8_superCatchUp_belowThreshold_participantChoice() {
            ParticipantInfo p = participant(1965, 7, new BigDecimal("130000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.SUPER_CATCH_UP)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("11250"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
            assertEquals(ContributionDesignation.PRE_TAX, result.getEffectiveDesignation());
        }

        @Test
        @DisplayName("Scenario 9: Age 64, $160K wages, $8,000 standard catch-up => ACCEPTED, Roth only, no super")
        void scenario9_age64_revertsToStandard_rothOnly() {
            ParticipantInfo p = participant(1962, 3, new BigDecimal("160000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.ROTH)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
            assertEquals(ContributionDesignation.ROTH, result.getEffectiveDesignation());
        }

        @Test
        @DisplayName("Scenario 10: Age 59, $160K wages, $8,000 standard catch-up => ACCEPTED, Roth only")
        void scenario10_age59_standardCatchUp_highEarner_rothOnly() {
            ParticipantInfo p = participant(1967, 6, new BigDecimal("160000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.ROTH)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
            assertEquals(ContributionDesignation.ROTH, result.getEffectiveDesignation());
        }
    }

    // =========================================================================
    // Edge Cases — Scenarios 11–14
    // =========================================================================

    @Nested
    @DisplayName("Edge Case Scenarios")
    class EdgeCases {

        @Test
        @DisplayName("Scenario 11: Turns 60 in October 2026 => eligible for super catch-up for full year")
        void scenario11_turns60inOctober_superCatchUpFullYear() {
            // Born October 1966 => attains age 60 in 2026
            ParticipantInfo p = ParticipantInfo.builder()
                    .participantId("P011")
                    .dateOfBirth(LocalDate.of(1966, 10, 15))
                    .priorYearFicaWages(List.of(new BigDecimal("160000")))
                    .build();

            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.SUPER_CATCH_UP)
                    .designation(ContributionDesignation.ROTH)
                    .amount(new BigDecimal("11250"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
            assertEquals(ContributionDesignation.ROTH, result.getEffectiveDesignation());
        }

        @Test
        @DisplayName("Scenario 12: Turns 64 in March 2026 => reverts to standard catch-up for full year")
        void scenario12_turns64inMarch_standardCatchUpFullYear() {
            // Born March 1962 => attains age 64 in 2026
            ParticipantInfo p = ParticipantInfo.builder()
                    .participantId("P012")
                    .dateOfBirth(LocalDate.of(1962, 3, 15))
                    .priorYearFicaWages(List.of(new BigDecimal("160000")))
                    .build();

            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.ROTH)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
            assertEquals(ContributionDesignation.ROTH, result.getEffectiveDesignation());
        }

        @Test
        @DisplayName("Scenario 12b: Age 64 with pre-tax catch-up => REJECTED (still high earner)")
        void scenario12b_age64_preTax_rejected() {
            ParticipantInfo p = ParticipantInfo.builder()
                    .participantId("P012b")
                    .dateOfBirth(LocalDate.of(1962, 3, 15))
                    .priorYearFicaWages(List.of(new BigDecimal("160000")))
                    .build();

            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertFalse(result.isAccepted());
            assertEquals(RoutingResult.ERROR_ROTH_CATCHUP_REQUIRED, result.getErrorCode());
        }

        @Test
        @DisplayName("Scenario 13: New hire, no prior-year W-2 => allow pre-tax catch-up (good-faith)")
        void scenario13_newHire_noPriorYearW2_allowPreTax() {
            ParticipantInfo p = participantNewHire(1974, 6);
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
            assertEquals(ContributionDesignation.PRE_TAX, result.getEffectiveDesignation());
        }

        @Test
        @DisplayName("Scenario 14: Controlled group $80K + $75K = $155K => affected (high earner)")
        void scenario14_controlledGroupAggregation_highEarner() {
            ParticipantInfo p = participantWithControlledGroup(1974, 6,
                    new BigDecimal("80000"), new BigDecimal("75000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertFalse(result.isAccepted());
            assertEquals(RoutingResult.ERROR_ROTH_CATCHUP_REQUIRED, result.getErrorCode());
        }

        @Test
        @DisplayName("Scenario 14b: Controlled group aggregation => Roth accepted")
        void scenario14b_controlledGroupAggregation_rothAccepted() {
            ParticipantInfo p = participantWithControlledGroup(1974, 6,
                    new BigDecimal("80000"), new BigDecimal("75000"));
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.ROTH)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
            assertEquals(ContributionDesignation.ROTH, result.getEffectiveDesignation());
        }
    }

    // =========================================================================
    // Governmental 457(b) Special Catch-Up Exemption
    // =========================================================================

    @Nested
    @DisplayName("Governmental 457(b) Special Catch-Up Exemption")
    class Governmental457b {

        @Test
        @DisplayName("457(b) participant in special catch-up window => exempt from Roth requirement")
        void specialCatchUp_inWindow_exempt() {
            // NRA = 65, age in 2026 = 62 => within 3-year window (62, 63, 64)
            ParticipantInfo p = participant457b(1964, 6, new BigDecimal("200000"), 65);
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertTrue(result.isAccepted());
            assertEquals(ContributionDesignation.PRE_TAX, result.getEffectiveDesignation());
        }

        @Test
        @DisplayName("457(b) participant outside special catch-up window => NOT exempt")
        void specialCatchUp_outsideWindow_notExempt() {
            // NRA = 65, age in 2026 = 52 => NOT in 3-year window
            ParticipantInfo p = participant457b(1974, 6, new BigDecimal("200000"), 65);
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertFalse(result.isAccepted());
            assertEquals(RoutingResult.ERROR_ROTH_CATCHUP_REQUIRED, result.getErrorCode());
        }

        @Test
        @DisplayName("457(b) participant at NRA => NOT in special catch-up window")
        void specialCatchUp_atNRA_notExempt() {
            // NRA = 65, age in 2026 = 65 => at NRA, window is 62-64
            ParticipantInfo p = participant457b(1961, 6, new BigDecimal("200000"), 65);
            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertFalse(result.isAccepted());
            assertEquals(RoutingResult.ERROR_ROTH_CATCHUP_REQUIRED, result.getErrorCode());
        }

        @Test
        @DisplayName("Non-457(b) plan => no special catch-up exemption")
        void non457b_noExemption() {
            ParticipantInfo p = ParticipantInfo.builder()
                    .participantId("TEST")
                    .dateOfBirth(LocalDate.of(1964, 6, 1))
                    .priorYearFicaWages(List.of(new BigDecimal("200000")))
                    .planType(PlanType.PLAN_401K)
                    .normalRetirementAge(65)
                    .build();

            ContributionRequest request = ContributionRequest.builder()
                    .participantInfo(p)
                    .contributionType(ContributionType.CATCH_UP)
                    .designation(ContributionDesignation.PRE_TAX)
                    .amount(new BigDecimal("8000"))
                    .planYear(2026)
                    .build();

            RoutingResult result = routingEngine.route(request);
            assertFalse(result.isAccepted());
            assertEquals(RoutingResult.ERROR_ROTH_CATCHUP_REQUIRED, result.getErrorCode());
        }
    }
}
