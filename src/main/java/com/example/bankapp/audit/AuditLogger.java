package com.example.bankapp.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class AuditLogger {

    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_FAILURE = "FAILURE";

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    public void success(String actor, String action, BigDecimal amount, Long fromAccountId, Long toAccountId) {
        write(actor, action, amount, fromAccountId, toAccountId, OUTCOME_SUCCESS, "");
    }

    public void failure(String actor, String action, BigDecimal amount, Long fromAccountId, Long toAccountId, String reason) {
        write(actor, action, amount, fromAccountId, toAccountId, OUTCOME_FAILURE, reason);
    }

    private void write(String actor, String action, BigDecimal amount, Long fromAccountId, Long toAccountId,
                       String outcome, String reason) {
        log.info("event=financial_transaction timestamp={} actor={} action={} amount={} fromAccountId={} toAccountId={} outcome={} reason=\"{}\"",
                Instant.now(), actor, action, amount, fromAccountId, toAccountId, outcome, reason);
    }
}
