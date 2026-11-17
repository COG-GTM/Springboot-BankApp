package com.example.bankapp.scheduler;

import com.example.bankapp.model.ReconciliationReport;
import com.example.bankapp.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AccountReconciliationJob {

    private static final Logger logger = LoggerFactory.getLogger(AccountReconciliationJob.class);

    @Autowired
    private ReconciliationService reconciliationService;

    @Scheduled(cron = "${reconciliation.cron:0 0 2 * * ?}")
    public void runDailyReconciliation() {
        logger.info("Starting daily account reconciliation...");
        try {
            ReconciliationReport report = reconciliationService.runReconciliation();
            logger.info("Reconciliation completed. Total: {}, Flagged: {}, Discrepancies: {}",
                report.getTotalAccounts(), report.getFlaggedAccounts(), report.getDiscrepancyAccounts());
        } catch (Exception e) {
            logger.error("Reconciliation failed", e);
        }
    }
}
