package com.example.bankapp.compliance.secure2.util;

import com.example.bankapp.compliance.secure2.model.CatchUpTier;

import java.time.LocalDate;

/**
 * Utility for age-related calculations per SECURE 2.0.
 * Age determination uses the age attained during the calendar year,
 * not the age at the date of contribution (IRC §414(v)).
 */
public final class AgeCalculationUtil {

    private static final int CATCH_UP_MIN_AGE = 50;
    private static final int SUPER_CATCH_UP_MIN_AGE = 60;
    private static final int SUPER_CATCH_UP_MAX_AGE = 63;

    private AgeCalculationUtil() {
    }

    /**
     * Returns the age the participant will attain (or has attained) during the given calendar year.
     * Per IRS rules, this is simply {@code calendarYear - birthYear}.
     */
    public static int ageAttainedInYear(LocalDate dateOfBirth, int calendarYear) {
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("dateOfBirth must not be null");
        }
        return calendarYear - dateOfBirth.getYear();
    }

    /**
     * Determines the catch-up contribution tier for a participant in a given plan year.
     * <ul>
     *   <li>Under 50: NONE (not eligible for catch-up)</li>
     *   <li>50–59: STANDARD catch-up</li>
     *   <li>60–63: SUPER catch-up (Section 109)</li>
     *   <li>64+: STANDARD_POST_SUPER (reverts to standard catch-up limits)</li>
     * </ul>
     */
    public static CatchUpTier determineCatchUpTier(LocalDate dateOfBirth, int planYear) {
        int age = ageAttainedInYear(dateOfBirth, planYear);

        if (age < CATCH_UP_MIN_AGE) {
            return CatchUpTier.NONE;
        } else if (age < SUPER_CATCH_UP_MIN_AGE) {
            return CatchUpTier.STANDARD;
        } else if (age <= SUPER_CATCH_UP_MAX_AGE) {
            return CatchUpTier.SUPER;
        } else {
            return CatchUpTier.STANDARD_POST_SUPER;
        }
    }

    /**
     * Returns true if the participant is eligible for any catch-up contributions
     * (age 50+ attained during the calendar year).
     */
    public static boolean isCatchUpEligible(LocalDate dateOfBirth, int planYear) {
        return ageAttainedInYear(dateOfBirth, planYear) >= CATCH_UP_MIN_AGE;
    }

    /**
     * Returns true if the participant qualifies for the enhanced (super) catch-up
     * under Section 109 (ages 60–63 attained during the calendar year).
     */
    public static boolean isSuperCatchUpEligible(LocalDate dateOfBirth, int planYear) {
        int age = ageAttainedInYear(dateOfBirth, planYear);
        return age >= SUPER_CATCH_UP_MIN_AGE && age <= SUPER_CATCH_UP_MAX_AGE;
    }
}
