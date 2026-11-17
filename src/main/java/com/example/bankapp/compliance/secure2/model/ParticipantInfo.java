package com.example.bankapp.compliance.secure2.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class ParticipantInfo {

    private final String participantId;
    private final LocalDate dateOfBirth;
    private final List<BigDecimal> priorYearFicaWages;
    private final PlanType planType;
    private final boolean newHire;
    private final Integer normalRetirementAge;

    private ParticipantInfo(Builder builder) {
        this.participantId = builder.participantId;
        this.dateOfBirth = builder.dateOfBirth;
        this.priorYearFicaWages = builder.priorYearFicaWages != null
                ? Collections.unmodifiableList(builder.priorYearFicaWages)
                : Collections.emptyList();
        this.planType = builder.planType;
        this.newHire = builder.newHire;
        this.normalRetirementAge = builder.normalRetirementAge;
    }

    public String getParticipantId() {
        return participantId;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public List<BigDecimal> getPriorYearFicaWages() {
        return priorYearFicaWages;
    }

    public PlanType getPlanType() {
        return planType;
    }

    public boolean isNewHire() {
        return newHire;
    }

    public Integer getNormalRetirementAge() {
        return normalRetirementAge;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String participantId;
        private LocalDate dateOfBirth;
        private List<BigDecimal> priorYearFicaWages;
        private PlanType planType = PlanType.PLAN_401K;
        private boolean newHire = false;
        private Integer normalRetirementAge;

        public Builder participantId(String participantId) {
            this.participantId = participantId;
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder priorYearFicaWages(List<BigDecimal> priorYearFicaWages) {
            this.priorYearFicaWages = priorYearFicaWages;
            return this;
        }

        public Builder planType(PlanType planType) {
            this.planType = planType;
            return this;
        }

        public Builder newHire(boolean newHire) {
            this.newHire = newHire;
            return this;
        }

        public Builder normalRetirementAge(Integer normalRetirementAge) {
            this.normalRetirementAge = normalRetirementAge;
            return this;
        }

        public ParticipantInfo build() {
            if (participantId == null || participantId.isBlank()) {
                throw new IllegalArgumentException("participantId is required");
            }
            if (dateOfBirth == null) {
                throw new IllegalArgumentException("dateOfBirth is required");
            }
            return new ParticipantInfo(this);
        }
    }
}
