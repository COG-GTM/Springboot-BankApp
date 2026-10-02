package com.cascade.disclosure.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Reads the DNA core's outputs. Default source "legacy" = the recorded golden set (produced by a reference model of
 * the Oracle behaviour derived from the PL/SQL source — not Oracle). Source "migrated" = the PostgreSQL run's export
 * (demo/out/bk/migrated_daily_accrual.csv), present only when the migrated run has been loaded.
 */
@Component
public class CoreOutputs {
    public record Product(String code, String accountType, String name, BigDecimal interestRate, String compounding,
                          BigDecimal monthlyFee, BigDecimal waiverMinBalance, BigDecimal disclosedApy) {}

    public record LoanProduct(String code, String name, BigDecimal nominalRate, int termMonths, BigDecimal amountFinanced,
                              BigDecimal prepaidFinanceCharge, BigDecimal monthlyPayment, BigDecimal disclosedApr) {}

    public record Account(long accountId, String accountType, String productCode, BigDecimal openingBalance, BigDecimal interestRate) {}

    public record AccrualRow(String asOfDate, long accountId, BigDecimal ledgerBalanceSod, BigDecimal dailyAccrual,
                             BigDecimal accrualCents, BigDecimal accruedInterestEod) {}

    public static final String LEGACY = "legacy";
    public static final String MIGRATED = "migrated";

    public List<Product> products() {
        return rows("legacy/products.csv").stream().map(r -> new Product(r.get("product_code"), r.get("account_type"), r.get("name"),
                new BigDecimal(r.get("interest_rate")), r.get("compounding"), new BigDecimal(r.get("monthly_fee")),
                new BigDecimal(r.get("waiver_min_balance")), new BigDecimal(r.get("disclosed_apy")))).toList();
    }

    public List<LoanProduct> loanProducts() {
        return rows("legacy/loan_products.csv").stream().map(r -> new LoanProduct(r.get("product_code"), r.get("name"),
                new BigDecimal(r.get("nominal_rate")), Integer.parseInt(r.get("term_months")), new BigDecimal(r.get("amount_financed")),
                new BigDecimal(r.get("prepaid_finance_charge")), new BigDecimal(r.get("monthly_payment")), new BigDecimal(r.get("disclosed_apr")))).toList();
    }

    public List<Account> accounts() {
        return rows("legacy/accounts.csv").stream().map(r -> new Account(Long.parseLong(r.get("account_id")), r.get("account_type"),
                r.get("product_code"), new BigDecimal(r.get("ledger_balance")), new BigDecimal(r.get("interest_rate")))).toList();
    }

    public boolean hasSource(String source) {
        return new ClassPathResource("core-outputs/" + source + "/daily_accrual.csv").exists();
    }

    public Optional<List<AccrualRow>> dailyAccrual(String source) {
        if (!hasSource(source)) {
            return Optional.empty();
        }
        return Optional.of(rows(source + "/daily_accrual.csv").stream().map(r -> new AccrualRow(r.get("as_of_dt"), Long.parseLong(r.get("account_id")),
                new BigDecimal(r.get("ledger_balance_sod")), new BigDecimal(r.get("daily_accrual")), new BigDecimal(r.get("accrual_cents")),
                new BigDecimal(r.get("accrued_interest_eod")))).toList());
    }

    static List<Map<String, String>> rows(String path) {
        ClassPathResource res = new ClassPathResource("core-outputs/" + path);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String header = in.readLine();
            if (header == null) {
                return List.of();
            }
            String[] cols = header.split(",");
            List<Map<String, String>> out = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] v = line.split(",", -1);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < cols.length; i++) {
                    row.put(cols[i], i < v.length ? v[i] : "");
                }
                out.add(row);
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read core output " + path, e);
        }
    }
}
