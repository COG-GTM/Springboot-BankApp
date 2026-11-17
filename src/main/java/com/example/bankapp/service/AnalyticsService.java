package com.example.bankapp.service;

import com.example.bankapp.dto.CategorySpendingDto;
import com.example.bankapp.dto.DailyTrendDto;
import com.example.bankapp.dto.MonthlySummaryDto;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AnalyticsService {

    static final String TYPE_DEPOSIT = "Deposit";
    static final String TYPE_WITHDRAWAL = "Withdrawal";
    static final String PREFIX_TRANSFER_OUT = "Transfer Out";
    static final String PREFIX_TRANSFER_IN = "Transfer In";

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Total spending grouped by transaction category for the given date range.
     * Spending = Withdrawals + outgoing Transfers.
     */
    @Transactional(readOnly = true)
    public List<CategorySpendingDto> getSpendingByCategory(String username, LocalDate startDate, LocalDate endDate) {
        List<Transaction> txns = loadTransactions(username, startDate, endDate);

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        Map<String, Long> counts = new LinkedHashMap<>();

        for (Transaction t : txns) {
            if (!isSpending(t.getType()) || t.getAmount() == null) {
                continue;
            }
            String category = normalizeCategory(t.getType());
            totals.merge(category, t.getAmount(), BigDecimal::add);
            counts.merge(category, 1L, Long::sum);
        }

        List<CategorySpendingDto> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : totals.entrySet()) {
            result.add(new CategorySpendingDto(e.getKey(), e.getValue(), counts.get(e.getKey())));
        }
        result.sort(Comparator.comparing(CategorySpendingDto::getTotal).reversed());
        return result;
    }

    /**
     * Income, expenses, and net balance for each month the account has activity.
     */
    @Transactional(readOnly = true)
    public List<MonthlySummaryDto> getMonthlySummary(String username) {
        Account account = accountService.findAccountByUsername(username);
        List<Transaction> txns = transactionRepository.findByAccountId(account.getId());

        Map<String, BigDecimal> income = new TreeMap<>();
        Map<String, BigDecimal> expenses = new TreeMap<>();

        for (Transaction t : txns) {
            if (t.getTimestamp() == null || t.getAmount() == null) {
                continue;
            }
            String key = t.getTimestamp().toLocalDate().format(MONTH_FORMAT);
            if (isSpending(t.getType())) {
                expenses.merge(key, t.getAmount(), BigDecimal::add);
            } else if (isIncome(t.getType())) {
                income.merge(key, t.getAmount(), BigDecimal::add);
            }
        }

        TreeMap<String, MonthlySummaryDto> merged = new TreeMap<>();
        for (String month : income.keySet()) {
            merged.put(month, buildMonthly(month, income.get(month), expenses.getOrDefault(month, BigDecimal.ZERO)));
        }
        for (String month : expenses.keySet()) {
            if (merged.containsKey(month)) continue;
            merged.put(month, buildMonthly(month, income.getOrDefault(month, BigDecimal.ZERO), expenses.get(month)));
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * Daily spending totals across the given date range. Includes zero-value days
     * so the resulting series is suitable for a continuous trend chart.
     */
    @Transactional(readOnly = true)
    public List<DailyTrendDto> getTrends(String username, LocalDate startDate, LocalDate endDate) {
        List<Transaction> txns = loadTransactions(username, startDate, endDate);

        Map<LocalDate, BigDecimal> daily = new TreeMap<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            daily.put(d, BigDecimal.ZERO);
        }
        for (Transaction t : txns) {
            if (!isSpending(t.getType()) || t.getTimestamp() == null || t.getAmount() == null) {
                continue;
            }
            LocalDate day = t.getTimestamp().toLocalDate();
            daily.merge(day, t.getAmount(), BigDecimal::add);
        }

        List<DailyTrendDto> result = new ArrayList<>(daily.size());
        for (Map.Entry<LocalDate, BigDecimal> e : daily.entrySet()) {
            result.add(new DailyTrendDto(e.getKey(), e.getValue()));
        }
        return result;
    }

    private List<Transaction> loadTransactions(String username, LocalDate startDate, LocalDate endDate) {
        Account account = accountService.findAccountByUsername(username);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        return transactionRepository.findByAccountIdAndTimestampBetween(account.getId(), start, end);
    }

    private MonthlySummaryDto buildMonthly(String month, BigDecimal income, BigDecimal expenses) {
        return new MonthlySummaryDto(month, income, expenses, income.subtract(expenses));
    }

    private boolean isSpending(String type) {
        if (type == null) return false;
        return type.equalsIgnoreCase(TYPE_WITHDRAWAL) || type.startsWith(PREFIX_TRANSFER_OUT);
    }

    private boolean isIncome(String type) {
        if (type == null) return false;
        return type.equalsIgnoreCase(TYPE_DEPOSIT) || type.startsWith(PREFIX_TRANSFER_IN);
    }

    private String normalizeCategory(String type) {
        if (type == null) return "Unknown";
        if (type.startsWith(PREFIX_TRANSFER_OUT)) return PREFIX_TRANSFER_OUT;
        if (type.startsWith(PREFIX_TRANSFER_IN)) return PREFIX_TRANSFER_IN;
        return type;
    }
}
