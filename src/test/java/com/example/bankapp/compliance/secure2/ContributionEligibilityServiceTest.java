package com.example.bankapp.compliance.secure2;

import com.example.bankapp.compliance.secure2.engine.ContributionLimitEngine;
import com.example.bankapp.compliance.secure2.model.EligibilityResult;
import com.example.bankapp.compliance.secure2.model.CatchUpTier;
import com.example.bankapp.compliance.secure2.model.ParticipantInfo;
import com.example.bankapp.compliance.secure2.model.PlanType;
import com.example.bankapp.compliance.secure2.service.ContributionEligibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContributionEligibilityServiceTest {

    private ContributionEligibilityService service;

    @BeforeEach
    void setUp() {
        ContributionLimitEngine engine = new ContributionLimitEngine();
        service = new ContributionEligibilityService(engine);
    }

    @Test
    void highEarner_above150k() {
        ParticipantInfo participant = ParticipantInfo.builder()
                .participantId("P001")
                .dateOfBirth(LocalDate.of(1974, 6, 15)) // age 52 in 2026
                .priorYearFicaWages(List.of(new BigDecimal("160000")))
                .build();

        EligibilityResult result = service.evaluate(participant, 2026);
        assertTrue(result.isCatchUpEligible());
        assertTrue(result.isHighEarner());
        assertEquals(CatchUpTier.STANDARD, result.getCatchUpTier());
        assertFalse(result.isGoodFaithDefault());
    }

    @Test
    void notHighEarner_below150k() {
        ParticipantInfo participant = ParticipantInfo.builder()
                .participantId("P002")
                .dateOfBirth(LocalDate.of(1974, 6, 15)) // age 52 in 2026
                .priorYearFicaWages(List.of(new BigDecimal("130000")))
                .build();

        EligibilityResult result = service.evaluate(participant, 2026);
        assertTrue(result.isCatchUpEligible());
        assertFalse(result.isHighEarner());
    }

    @Test
    void scenario6_exactlyAtThreshold_notHighEarner() {
        // $145,000 exactly at 2025 threshold (for 2025 plan year)
        // For 2026: threshold is $150,000, test at exactly $150,000
        ParticipantInfo participant = ParticipantInfo.builder()
                .participantId("P006")
                .dateOfBirth(LocalDate.of(1974, 6, 15))
                .priorYearFicaWages(List.of(new BigDecimal("150000")))
                .build();

        EligibilityResult result = service.evaluate(participant, 2026);
        // "In excess of" = strictly greater than, so exactly $150,000 is NOT high earner
        assertFalse(result.isHighEarner());
    }

    @Test
    void scenario13_newHire_goodFaithDefault() {
        ParticipantInfo participant = ParticipantInfo.builder()
                .participantId("P013")
                .dateOfBirth(LocalDate.of(1974, 6, 15))
                .newHire(true)
                .priorYearFicaWages(Collections.emptyList())
                .build();

        EligibilityResult result = service.evaluate(participant, 2026);
        assertFalse(result.isHighEarner());
        assertTrue(result.isGoodFaithDefault());
        assertTrue(result.isCatchUpEligible());
    }

    @Test
    void scenario14_controlledGroupAggregation() {
        // $80K + $75K = $155K > $150K threshold
        ParticipantInfo participant = ParticipantInfo.builder()
                .participantId("P014")
                .dateOfBirth(LocalDate.of(1974, 6, 15))
                .priorYearFicaWages(Arrays.asList(
                        new BigDecimal("80000"),
                        new BigDecimal("75000")))
                .build();

        EligibilityResult result = service.evaluate(participant, 2026);
        assertTrue(result.isHighEarner());
        assertEquals(0, result.getAggregatedFicaWages().compareTo(new BigDecimal("155000")));
    }

    @Test
    void superCatchUp_age61_highEarner() {
        ParticipantInfo participant = ParticipantInfo.builder()
                .participantId("P007")
                .dateOfBirth(LocalDate.of(1965, 7, 1)) // age 61 in 2026
                .priorYearFicaWages(List.of(new BigDecimal("160000")))
                .build();

        EligibilityResult result = service.evaluate(participant, 2026);
        assertTrue(result.isHighEarner());
        assertEquals(CatchUpTier.SUPER, result.getCatchUpTier());
        // Super catch-up limit for 2026: $12,000 (150% of $8,000)
        assertEquals(0, result.getApplicableCatchUpLimit().compareTo(new BigDecimal("12000.0")));
    }

    @Test
    void age64_revertsToStandard() {
        ParticipantInfo participant = ParticipantInfo.builder()
                .participantId("P009")
                .dateOfBirth(LocalDate.of(1962, 3, 1)) // age 64 in 2026
                .priorYearFicaWages(List.of(new BigDecimal("160000")))
                .build();

        EligibilityResult result = service.evaluate(participant, 2026);
        assertTrue(result.isHighEarner());
        assertEquals(CatchUpTier.STANDARD_POST_SUPER, result.getCatchUpTier());
        assertEquals(0, result.getApplicableCatchUpLimit().compareTo(new BigDecimal("8000")));
    }

    @Test
    void under50_notCatchUpEligible() {
        ParticipantInfo participant = ParticipantInfo.builder()
                .participantId("P005")
                .dateOfBirth(LocalDate.of(1978, 5, 1)) // age 48 in 2026
                .priorYearFicaWages(List.of(new BigDecimal("200000")))
                .build();

        EligibilityResult result = service.evaluate(participant, 2026);
        assertFalse(result.isCatchUpEligible());
        assertEquals(CatchUpTier.NONE, result.getCatchUpTier());
    }

    @Test
    void emptyWages_treatedAsGoodFaith() {
        ParticipantInfo participant = ParticipantInfo.builder()
                .participantId("P099")
                .dateOfBirth(LocalDate.of(1974, 6, 15))
                .priorYearFicaWages(Collections.emptyList())
                .build();

        EligibilityResult result = service.evaluate(participant, 2026);
        assertFalse(result.isHighEarner());
        assertTrue(result.isGoodFaithDefault());
    }
}
