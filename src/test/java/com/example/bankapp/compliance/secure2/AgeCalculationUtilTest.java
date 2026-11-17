package com.example.bankapp.compliance.secure2;

import com.example.bankapp.compliance.secure2.model.CatchUpTier;
import com.example.bankapp.compliance.secure2.util.AgeCalculationUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AgeCalculationUtilTest {

    @Test
    void ageAttainedInYear_standardCase() {
        // Born 1974 => age 52 in 2026
        assertEquals(52, AgeCalculationUtil.ageAttainedInYear(
                LocalDate.of(1974, 6, 15), 2026));
    }

    @Test
    void ageAttainedInYear_birthdayLaterInYear() {
        // Born Oct 1966, turning 60 in Oct 2026 => age attained = 60 for full year
        assertEquals(60, AgeCalculationUtil.ageAttainedInYear(
                LocalDate.of(1966, 10, 15), 2026));
    }

    @Test
    void ageAttainedInYear_birthdayEarlyInYear() {
        // Born Mar 1962, turning 64 in Mar 2026 => age attained = 64
        assertEquals(64, AgeCalculationUtil.ageAttainedInYear(
                LocalDate.of(1962, 3, 1), 2026));
    }

    @Test
    void determineCatchUpTier_under50() {
        // Age 48 in 2026
        assertEquals(CatchUpTier.NONE,
                AgeCalculationUtil.determineCatchUpTier(
                        LocalDate.of(1978, 5, 1), 2026));
    }

    @Test
    void determineCatchUpTier_standard50to59() {
        // Age 52 in 2026
        assertEquals(CatchUpTier.STANDARD,
                AgeCalculationUtil.determineCatchUpTier(
                        LocalDate.of(1974, 1, 1), 2026));
        // Age 59 in 2026
        assertEquals(CatchUpTier.STANDARD,
                AgeCalculationUtil.determineCatchUpTier(
                        LocalDate.of(1967, 12, 31), 2026));
    }

    @Test
    void determineCatchUpTier_super60to63() {
        // Age 60 in 2026
        assertEquals(CatchUpTier.SUPER,
                AgeCalculationUtil.determineCatchUpTier(
                        LocalDate.of(1966, 10, 15), 2026));
        // Age 61 in 2026
        assertEquals(CatchUpTier.SUPER,
                AgeCalculationUtil.determineCatchUpTier(
                        LocalDate.of(1965, 7, 1), 2026));
        // Age 63 in 2026
        assertEquals(CatchUpTier.SUPER,
                AgeCalculationUtil.determineCatchUpTier(
                        LocalDate.of(1963, 1, 1), 2026));
    }

    @Test
    void determineCatchUpTier_standardPostSuper64Plus() {
        // Age 64 in 2026
        assertEquals(CatchUpTier.STANDARD_POST_SUPER,
                AgeCalculationUtil.determineCatchUpTier(
                        LocalDate.of(1962, 3, 1), 2026));
        // Age 70 in 2026
        assertEquals(CatchUpTier.STANDARD_POST_SUPER,
                AgeCalculationUtil.determineCatchUpTier(
                        LocalDate.of(1956, 6, 1), 2026));
    }

    @Test
    void isCatchUpEligible_ageThresholds() {
        // Age 49 — not eligible
        assertFalse(AgeCalculationUtil.isCatchUpEligible(
                LocalDate.of(1977, 1, 1), 2026));
        // Age 50 — eligible
        assertTrue(AgeCalculationUtil.isCatchUpEligible(
                LocalDate.of(1976, 12, 31), 2026));
    }

    @Test
    void isSuperCatchUpEligible_boundaries() {
        // Age 59 — not super
        assertFalse(AgeCalculationUtil.isSuperCatchUpEligible(
                LocalDate.of(1967, 12, 31), 2026));
        // Age 60 — super
        assertTrue(AgeCalculationUtil.isSuperCatchUpEligible(
                LocalDate.of(1966, 10, 15), 2026));
        // Age 63 — super
        assertTrue(AgeCalculationUtil.isSuperCatchUpEligible(
                LocalDate.of(1963, 1, 1), 2026));
        // Age 64 — not super
        assertFalse(AgeCalculationUtil.isSuperCatchUpEligible(
                LocalDate.of(1962, 3, 1), 2026));
    }

    @Test
    void ageAttainedInYear_nullDateOfBirth_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> AgeCalculationUtil.ageAttainedInYear(null, 2026));
    }

    // --- Ticket Edge Cases ---

    @Test
    void scenario11_turns60inOctober2026_superCatchUpFullYear() {
        // Participant turns 60 in October 2026
        LocalDate dob = LocalDate.of(1966, 10, 15);
        assertTrue(AgeCalculationUtil.isSuperCatchUpEligible(dob, 2026));
        assertEquals(CatchUpTier.SUPER,
                AgeCalculationUtil.determineCatchUpTier(dob, 2026));
    }

    @Test
    void scenario12_turns64inMarch2026_standardForFullYear() {
        // Participant turns 64 in March 2026
        LocalDate dob = LocalDate.of(1962, 3, 15);
        assertEquals(CatchUpTier.STANDARD_POST_SUPER,
                AgeCalculationUtil.determineCatchUpTier(dob, 2026));
        assertFalse(AgeCalculationUtil.isSuperCatchUpEligible(dob, 2026));
    }
}
