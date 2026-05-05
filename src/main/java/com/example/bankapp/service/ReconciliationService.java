package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.ReconciliationReport;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.ReconciliationReportRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ReconciliationReportRepository reportRepository;

    @Transactional
    public ReconciliationReport runReconciliation() {
        List<Account> allAccounts = accountRepository.findAll();

        int flaggedCount = 0;
        int discrepancyCount = 0;
        BigDecimal totalBalance = BigDecimal.ZERO;
        StringBuilder detailsBuilder = new StringBuilder();
        detailsBuilder.append("[");

        for (int i = 0; i < allAccounts.size(); i++) {
            Account account = allAccounts.get(i);

            List<Transaction> transactions = transactionRepository
                .findByAccountIdOrderByTimestampAsc(account.getId());

            BigDecimal computedBalance = BigDecimal.ZERO;
            for (Transaction tx : transactions) {
                String type = tx.getType();
                if ("Deposit".equals(type) || (type != null && type.startsWith("Transfer In"))) {
                    computedBalance = computedBalance.add(tx.getAmount());
                } else {
                    computedBalance = computedBalance.subtract(tx.getAmount());
                }
            }

            BigDecimal storedBalance = account.getBalance();
            boolean hasDiscrepancy = computedBalance.compareTo(storedBalance) != 0;
            if (hasDiscrepancy) {
                discrepancyCount++;
                logger.warn("Discrepancy detected for account id={} username={} stored={} computed={}",
                    account.getId(), account.getUsername(), storedBalance, computedBalance);
            }

            boolean isNegative = storedBalance.compareTo(BigDecimal.ZERO) < 0;
            if (isNegative) {
                flaggedCount++;
                logger.warn("Negative balance flagged for account id={} username={} balance={}",
                    account.getId(), account.getUsername(), storedBalance);
            }

            totalBalance = totalBalance.add(storedBalance);

            if (i > 0) {
                detailsBuilder.append(",");
            }
            detailsBuilder.append(String.format(
                "{\"accountId\":%d,\"username\":\"%s\",\"storedBalance\":%s,\"computedBalance\":%s,\"discrepancy\":%s,\"negative\":%s}",
                account.getId(),
                escapeJson(account.getUsername()),
                storedBalance,
                computedBalance,
                hasDiscrepancy,
                isNegative
            ));
        }
        detailsBuilder.append("]");

        ReconciliationReport report = new ReconciliationReport();
        report.setReportDate(LocalDateTime.now());
        report.setTotalAccounts(allAccounts.size());
        report.setFlaggedAccounts(flaggedCount);
        report.setDiscrepancyAccounts(discrepancyCount);
        report.setTotalBalance(totalBalance);
        report.setStatus("COMPLETED");
        report.setDetails(detailsBuilder.toString());

        ReconciliationReport saved = reportRepository.save(report);

        logger.info("Reconciliation summary: totalAccounts={}, flaggedAccounts={}, discrepancyAccounts={}, totalBalance={}",
            saved.getTotalAccounts(), saved.getFlaggedAccounts(), saved.getDiscrepancyAccounts(), saved.getTotalBalance());

        return saved;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '"':
                    out.append("\\\"");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }
}
