package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account accountWithBalance(String balance) {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("alice");
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    @Test
    void hasSufficientFundsIsTrueWhenBalanceEqualsAmount() {
        Account account = accountWithBalance("100.00");

        assertTrue(accountService.hasSufficientFunds(account, new BigDecimal("100.00")));
        assertFalse(accountService.hasSufficientFunds(account, new BigDecimal("100.01")));
    }

    @Test
    void withdrawingTheFullBalanceSucceeds() {
        Account account = accountWithBalance("100.00");

        accountService.withdraw(account, new BigDecimal("100.00"));

        assertEquals(0, account.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void dateRangeHistoryIncludesTransactionsOnTheEndDay() {
        Account account = accountWithBalance("0");
        Transaction beforeRange = new Transaction(BigDecimal.ONE, "Deposit", LocalDateTime.of(2026, 11, 9, 23, 59), account);
        Transaction onStartDay = new Transaction(BigDecimal.ONE, "Deposit", LocalDateTime.of(2026, 11, 10, 0, 0), account);
        Transaction onEndDay = new Transaction(BigDecimal.ONE, "Deposit", LocalDateTime.of(2026, 11, 12, 15, 30), account);
        Transaction afterRange = new Transaction(BigDecimal.ONE, "Deposit", LocalDateTime.of(2026, 11, 13, 0, 0), account);
        when(transactionRepository.findByAccountId(1L))
                .thenReturn(List.of(afterRange, onEndDay, beforeRange, onStartDay));

        List<Transaction> result = accountService.getTransactionHistory(
                account, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12));

        assertEquals(List.of(onStartDay, onEndDay), result);
    }

    @Test
    void singleDayRangeReturnsThatDaysTransactions() {
        Account account = accountWithBalance("0");
        Transaction midday = new Transaction(BigDecimal.ONE, "Deposit", LocalDateTime.of(2026, 11, 10, 12, 0), account);
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(midday));

        LocalDate day = LocalDate.of(2026, 11, 10);

        assertEquals(List.of(midday), accountService.getTransactionHistory(account, day, day));
    }
}
