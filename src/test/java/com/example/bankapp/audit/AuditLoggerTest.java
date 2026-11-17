package com.example.bankapp.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLoggerTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger auditLogger;

    @BeforeEach
    void attachAppender() {
        auditLogger = (Logger) LoggerFactory.getLogger("audit");
        auditLogger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        auditLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        auditLogger.detachAppender(appender);
        MDC.clear();
    }

    @Test
    void writesEveryFieldOfASuccessfulTransfer() {
        MDC.put(CorrelationIdFilter.CORRELATION_ID_KEY, "corr-123");

        new AuditLogger().log(AuditEvent.builder("TRANSFER", AuditEvent.Outcome.SUCCESS)
                .actor("alice")
                .accountId(1L)
                .counterpartyAccountId(2L)
                .amount(new BigDecimal("25.50"))
                .timestamp(Instant.parse("2026-01-01T00:00:00Z"))
                .build());

        assertThat(message()).isEqualTo("{\"timestamp\":\"2026-01-01T00:00:00Z\",\"event\":\"TRANSFER\","
                + "\"outcome\":\"SUCCESS\",\"actor\":\"alice\",\"accountId\":\"1\",\"counterpartyAccountId\":\"2\","
                + "\"amount\":\"25.50\",\"currency\":\"USD\",\"correlationId\":\"corr-123\"}");
    }

    @Test
    void recordsTheReasonOfAFailedWithdrawalAndOmitsUnsetFields() {
        new AuditLogger().log(AuditEvent.builder("WITHDRAWAL", AuditEvent.Outcome.FAILURE)
                .actor("bob")
                .accountId(7L)
                .amount(new BigDecimal("10.00"))
                .reason("INSUFFICIENT_FUNDS")
                .build());

        assertThat(message())
                .contains("\"event\":\"WITHDRAWAL\"", "\"outcome\":\"FAILURE\"", "\"reason\":\"INSUFFICIENT_FUNDS\"")
                .doesNotContain("counterpartyAccountId", "correlationId");
    }

    @Test
    void escapesInjectedControlCharactersSoTheLogStaysOneRecordPerEvent() {
        new AuditLogger().log(AuditEvent.builder("DEPOSIT", AuditEvent.Outcome.FAILURE)
                .actor("mallory\"\n{\"event\":\"forged\"")
                .accountId(9L)
                .build());

        assertThat(message()).doesNotContain("\n");
        assertThat(message()).contains("\\\"event\\\":\\\"forged\\\"");
    }

    private String message() {
        assertThat(appender.list).hasSize(1);
        return appender.list.get(0).getFormattedMessage();
    }
}
