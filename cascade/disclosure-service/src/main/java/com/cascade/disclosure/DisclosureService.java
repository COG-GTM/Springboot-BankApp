package com.cascade.disclosure;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cascade.disclosure.calc.AprCalculator;
import com.cascade.disclosure.calc.ApyCalculator;
import com.cascade.disclosure.core.CoreOutputs;

/** Disclosure values from core outputs. Engineering evidence for Compliance — not a regulatory determination. */
@Service
public class DisclosureService {
    public record ProductApy(String productCode, String name, BigDecimal nominalRatePct, BigDecimal disclosedApy, BigDecimal appendixAApy,
                             boolean withinTolerance, String citation) {}

    public record LoanApr(String productCode, String name, BigDecimal nominalRatePct, BigDecimal monthlyPayment, BigDecimal disclosedApr,
                          BigDecimal actuarialApr, boolean withinTolerance, String citation) {}

    public record ApyEarned(long accountId, String productCode, String source, int days, BigDecimal avgBalance, BigDecimal interestEarned,
                            BigDecimal apyEarned) {}

    public record ParityRow(long accountId, String productCode, BigDecimal interestLegacy, BigDecimal interestMigrated, BigDecimal apyLegacy,
                            BigDecimal apyMigrated, BigDecimal deltaPp, boolean withinTolerance, String disposition) {}

    public record Parity(boolean migratedLoaded, int accounts, int differing, int exceedingTolerance, BigDecimal tolerancePp, List<ParityRow> rows,
                         String note) {}

    private final CoreOutputs core;
    private final BigDecimal apyTolerance;
    private final BigDecimal aprTolerance;

    public DisclosureService(CoreOutputs core,
                             @Value("${cascade.disclosure.apy-tolerance-pp:0.05}") BigDecimal apyTolerance,
                             @Value("${cascade.disclosure.apr-tolerance-pp:0.125}") BigDecimal aprTolerance) {
        this.core = core;
        this.apyTolerance = apyTolerance;
        this.aprTolerance = aprTolerance;
    }

    public List<ProductApy> productApys() {
        List<ProductApy> out = new ArrayList<>();
        for (CoreOutputs.Product p : core.products()) {
            BigDecimal appA = ApyCalculator.apyFromNominalRate(p.interestRate(), 365);
            out.add(new ProductApy(p.code(), p.name(), p.interestRate().movePointRight(2).setScale(2, RoundingMode.HALF_UP), p.disclosedApy(), appA,
                    ApyCalculator.withinTolerance(p.disclosedApy(), appA, apyTolerance), "12 CFR 1030 App. A Part I; §1030.3(f)(1)-(2)"));
        }
        return out;
    }

    public List<LoanApr> loanAprs() {
        List<LoanApr> out = new ArrayList<>();
        for (CoreOutputs.LoanProduct l : core.loanProducts()) {
            BigDecimal apr = AprCalculator.actuarialApr(l.amountFinanced().subtract(l.prepaidFinanceCharge()), l.monthlyPayment(), l.termMonths());
            out.add(new LoanApr(l.code(), l.name(), l.nominalRate().movePointRight(2).setScale(2, RoundingMode.HALF_UP), l.monthlyPayment(), l.disclosedApr(), apr,
                    AprCalculator.withinTolerance(l.disclosedApr(), apr, aprTolerance), "12 CFR §1026.22(a)(1)-(2)"));
        }
        return out;
    }

    /** APY earned per account over the loaded window (Appendix A Part II) from the statement's rounded daily cents. */
    public Map<Long, ApyEarned> apyEarned(String source) {
        Optional<List<CoreOutputs.AccrualRow>> rows = core.dailyAccrual(source);
        Map<Long, ApyEarned> out = new LinkedHashMap<>();
        if (rows.isEmpty()) {
            return out;
        }
        Map<Long, String> product = new LinkedHashMap<>();
        core.accounts().forEach(a -> product.put(a.accountId(), a.productCode()));
        Map<Long, BigDecimal[]> acc = new LinkedHashMap<>();   // [sum cents, sum balance, days]
        for (CoreOutputs.AccrualRow r : rows.get()) {
            BigDecimal[] a = acc.computeIfAbsent(r.accountId(), k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            a[0] = a[0].add(r.accrualCents());
            a[1] = a[1].add(r.ledgerBalanceSod());
            a[2] = a[2].add(BigDecimal.ONE);
        }
        acc.forEach((id, a) -> {
            int days = a[2].intValue();
            BigDecimal avg = a[1].divide(a[2], 2, RoundingMode.HALF_UP);
            out.put(id, new ApyEarned(id, product.get(id), source, days, avg, a[0], ApyCalculator.apy(a[0], avg, days)));
        });
        return out;
    }

    public Optional<ApyEarned> apyEarned(String source, long accountId) {
        return Optional.ofNullable(apyEarned(source).get(accountId));
    }

    /** Legacy vs migrated APY earned per account. Differences are reported, never rounded away. */
    public Parity parity() {
        Map<Long, ApyEarned> legacy = apyEarned(CoreOutputs.LEGACY);
        if (!core.hasSource(CoreOutputs.MIGRATED)) {
            return new Parity(false, legacy.size(), 0, 0, apyTolerance, List.of(),
                    "no migrated run loaded — add core-outputs/migrated/daily_accrual.csv (demo/out/bk/migrated_daily_accrual.csv from the parity step)");
        }
        Map<Long, ApyEarned> migrated = apyEarned(CoreOutputs.MIGRATED);
        List<ParityRow> rows = new ArrayList<>();
        int exceeding = 0;
        for (ApyEarned l : legacy.values()) {
            ApyEarned m = migrated.get(l.accountId());
            if (m == null || l.interestEarned().compareTo(m.interestEarned()) == 0) {
                continue;
            }
            BigDecimal delta = m.apyEarned().subtract(l.apyEarned());
            boolean within = delta.abs().compareTo(apyTolerance) <= 0;
            if (!within) {
                exceeding++;
            }
            rows.add(new ParityRow(l.accountId(), l.productCode(), l.interestEarned(), m.interestEarned(), l.apyEarned(), m.apyEarned(), delta, within,
                    within ? "ESCALATED — within §1030.3(f)(2) tolerance, still a behaviour change [compliance to confirm]"
                           : "ESCALATED — exceeds §1030.3(f)(2) tolerance [compliance to confirm]"));
        }
        return new Parity(true, legacy.size(), rows.size(), exceeding, apyTolerance, rows,
                "legacy = recorded golden set (reference model of the Oracle behaviour, not Oracle); migrated = PostgreSQL run");
    }
}
