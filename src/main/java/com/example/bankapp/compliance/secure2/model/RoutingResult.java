package com.example.bankapp.compliance.secure2.model;

public class RoutingResult {

    public static final String ERROR_ROTH_CATCHUP_REQUIRED = "ROTH_CATCHUP_REQUIRED";
    public static final String ERROR_NOT_CATCHUP_ELIGIBLE = "NOT_CATCHUP_ELIGIBLE";

    private final boolean accepted;
    private final ContributionDesignation effectiveDesignation;
    private final String errorCode;
    private final String message;

    private RoutingResult(Builder builder) {
        this.accepted = builder.accepted;
        this.effectiveDesignation = builder.effectiveDesignation;
        this.errorCode = builder.errorCode;
        this.message = builder.message;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public ContributionDesignation getEffectiveDesignation() {
        return effectiveDesignation;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public static RoutingResult accepted(ContributionDesignation designation, String message) {
        return new Builder()
                .accepted(true)
                .effectiveDesignation(designation)
                .message(message)
                .build();
    }

    public static RoutingResult rejected(String errorCode, String message) {
        return new Builder()
                .accepted(false)
                .errorCode(errorCode)
                .message(message)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean accepted;
        private ContributionDesignation effectiveDesignation;
        private String errorCode;
        private String message;

        public Builder accepted(boolean accepted) {
            this.accepted = accepted;
            return this;
        }

        public Builder effectiveDesignation(ContributionDesignation effectiveDesignation) {
            this.effectiveDesignation = effectiveDesignation;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public RoutingResult build() {
            return new RoutingResult(this);
        }
    }
}
