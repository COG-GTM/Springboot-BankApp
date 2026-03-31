package com.example.bankapp.compliance.secure2.model;

import java.math.BigDecimal;

public class ContributionRequest {

    private final ParticipantInfo participantInfo;
    private final ContributionType contributionType;
    private final ContributionDesignation designation;
    private final BigDecimal amount;
    private final int planYear;

    private ContributionRequest(Builder builder) {
        this.participantInfo = builder.participantInfo;
        this.contributionType = builder.contributionType;
        this.designation = builder.designation;
        this.amount = builder.amount;
        this.planYear = builder.planYear;
    }

    public ParticipantInfo getParticipantInfo() {
        return participantInfo;
    }

    public ContributionType getContributionType() {
        return contributionType;
    }

    public ContributionDesignation getDesignation() {
        return designation;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getPlanYear() {
        return planYear;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ParticipantInfo participantInfo;
        private ContributionType contributionType;
        private ContributionDesignation designation;
        private BigDecimal amount;
        private int planYear;

        public Builder participantInfo(ParticipantInfo participantInfo) {
            this.participantInfo = participantInfo;
            return this;
        }

        public Builder contributionType(ContributionType contributionType) {
            this.contributionType = contributionType;
            return this;
        }

        public Builder designation(ContributionDesignation designation) {
            this.designation = designation;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder planYear(int planYear) {
            this.planYear = planYear;
            return this;
        }

        public ContributionRequest build() {
            if (participantInfo == null) {
                throw new IllegalArgumentException("participantInfo is required");
            }
            if (contributionType == null) {
                throw new IllegalArgumentException("contributionType is required");
            }
            if (designation == null) {
                throw new IllegalArgumentException("designation is required");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
            if (planYear < 2026) {
                throw new IllegalArgumentException("SECURE 2.0 §603 applies to plan years beginning after 2025");
            }
            return new ContributionRequest(this);
        }
    }
}
