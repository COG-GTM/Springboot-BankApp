package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private Account alice;

    @BeforeEach
    void setUp() {
        alice = new Account();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setBalance(new BigDecimal("100.00"));
    }

    @Test
    void hasSufficientFundsIsTrueWhenBalanceEqualsAmount() {
        assertTrue(accountService.hasSufficientFunds(alice, new BigDecimal("100.00")));
    }

    @Test
    void withdrawingTheEntireBalanceSucceeds() {
        accountService.withdraw(alice, new BigDecimal("100.00"));

        assertEquals(0, alice.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void transferringTheEntireBalanceSucceeds() {
        Account bob = new Account();
        bob.setId(2L);
        bob.setUsername("bob");
        bob.setBalance(BigDecimal.ZERO);
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        accountService.transferAmount(alice, "bob", new BigDecimal("100.00"));

        assertEquals(0, alice.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, bob.getBalance().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void transactionHistoryRangeIncludesTransactionsOnTheEndDay() {
        LocalDate day = LocalDate.of(2026, 11, 20);
        Transaction morning = new Transaction(BigDecimal.TEN, "Deposit", day.atTime(9, 0), alice);
        Transaction evening = new Transaction(BigDecimal.ONE, "Withdrawal", day.atTime(18, 30), alice);
        Transaction nextDay = new Transaction(BigDecimal.ONE, "Deposit", day.plusDays(1).atStartOfDay(), alice);
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(evening, nextDay, morning));

        List<Transaction> result = accountService.getTransactionHistory(alice, day, day);

        assertEquals(List.of(morning, evening), result);
    }

    @Test
    void transactionHistoryRangeIncludesLastMomentOfEndDay() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);
        Transaction lastMoment = new Transaction(BigDecimal.TEN, "Deposit", LocalDateTime.of(2026, 11, 30, 23, 59, 59, 999_999_999), alice);
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(lastMoment));

        assertEquals(List.of(lastMoment), accountService.getTransactionHistory(alice, from, to));
    }

    @Test
    void loadUserByUsernameThrowsUsernameNotFoundForUnknownUser() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> accountService.loadUserByUsername("ghost"));
    }
}
