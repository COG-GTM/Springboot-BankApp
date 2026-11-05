package com.example.bankapp.audit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable description of a financial event to be written to the audit log.
 *
 * <p>Only identifiers, amounts and outcomes are carried here. Credentials, password hashes,
 * balances and free-form user input are deliberately excluded.
 */
public final class AuditEvent {

    public enum Outcome {
        SUCCESS,
        FAILURE
    }

    private final String eventType;
    private final Outcome outcome;
    private final String actor;
    private final Long accountId;
    private final Long counterpartyAccountId;
    private final BigDecimal amount;
    private final String currency;
    private final String reason;
    private final Instant timestamp;

    private AuditEvent(Builder builder) {
        this.eventType = builder.eventType;
        this.outcome = builder.outcome;
        this.actor = builder.actor;
        this.accountId = builder.accountId;
        this.counterpartyAccountId = builder.counterpartyAccountId;
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.reason = builder.reason;
        this.timestamp = builder.timestamp == null ? Instant.now() : builder.timestamp;
    }

    public static Builder builder(String eventType, Outcome outcome) {
        return new Builder(eventType, outcome);
    }

    public String eventType() {
        return eventType;
    }

    public Outcome outcome() {
        return outcome;
    }

    /**
     * Ordered field map used to render the event; null values are omitted by the renderer.
     */
    Map<String, Object> fields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("timestamp", timestamp.toString());
        fields.put("event", eventType);
        fields.put("outcome", outcome.name());
        fields.put("actor", actor);
        fields.put("accountId", accountId);
        fields.put("counterpartyAccountId", counterpartyAccountId);
        fields.put("amount", amount == null ? null : amount.toPlainString());
        fields.put("currency", currency);
        fields.put("reason", reason);
        return fields;
    }

    public static final class Builder {

        private static final String DEFAULT_CURRENCY = "USD";

        private final String eventType;
        private final Outcome outcome;
        private String actor;
        private Long accountId;
        private Long counterpartyAccountId;
        private BigDecimal amount;
        private String currency = DEFAULT_CURRENCY;
        private String reason;
        private Instant timestamp;

        private Builder(String eventType, Outcome outcome) {
            this.eventType = eventType;
            this.outcome = outcome;
        }

        public Builder actor(String actor) {
            this.actor = actor;
            return this;
        }

        public Builder accountId(Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder counterpartyAccountId(Long counterpartyAccountId) {
            this.counterpartyAccountId = counterpartyAccountId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AuditEvent build() {
            return new AuditEvent(this);
        }
    }
}
