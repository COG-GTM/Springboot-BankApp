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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account alice;
    private Account bob;

    @BeforeEach
    void setUp() {
        alice = account(1L, "alice", "100.00");
        bob = account(2L, "bob", "50.00");
    }

    private static Account account(Long id, String username, String balance) {
        Account a = new Account();
        a.setId(id);
        a.setUsername(username);
        a.setBalance(new BigDecimal(balance));
        return a;
    }

    private Transaction tx(LocalDateTime when) {
        return new Transaction(BigDecimal.ONE, "Deposit", when, alice);
    }

    @Test
    void hasSufficientFundsIsTrueWhenBalanceEqualsAmount() {
        assertTrue(accountService.hasSufficientFunds(alice, new BigDecimal("100.00")));
    }

    @Test
    void withdrawingEntireBalanceSucceeds() {
        accountService.withdraw(alice, new BigDecimal("100.00"));
        assertEquals(0, alice.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void transferringEntireBalanceSucceeds() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        accountService.transferAmount(alice, "bob", new BigDecimal("100.00"));
        assertEquals(0, alice.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, bob.getBalance().compareTo(new BigDecimal("150.00")));
    }

    @Test
    void dateRangeHistoryIncludesTransactionsOnTheEndDate() {
        Transaction onEndDay = tx(LocalDateTime.of(2026, 11, 20, 15, 30));
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(onEndDay));

        List<Transaction> result = accountService.getTransactionHistory(
                alice, LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 20));

        assertEquals(List.of(onEndDay), result);
    }

    @Test
    void singleDayRangeReturnsThatDaysTransactions() {
        Transaction sameDay = tx(LocalDateTime.of(2026, 11, 20, 9, 0));
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(sameDay));

        List<Transaction> result = accountService.getTransactionHistory(
                alice, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20));

        assertEquals(List.of(sameDay), result);
    }

    @Test
    void dateRangeHistoryExcludesDayAfterEndDateAndSortsOldestFirst() {
        Transaction later = tx(LocalDateTime.of(2026, 11, 19, 12, 0));
        Transaction earlier = tx(LocalDateTime.of(2026, 11, 18, 0, 0));
        Transaction nextDay = tx(LocalDateTime.of(2026, 11, 21, 0, 0));
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(later, nextDay, earlier));

        List<Transaction> result = accountService.getTransactionHistory(
                alice, LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 20));

        assertEquals(List.of(earlier, later), result);
    }

    @Test
    void negativeTransferIsRejectedAndDoesNotMoveFunds() {
        lenient().when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(alice, "bob", new BigDecimal("-50.00")));

        assertEquals(0, alice.getBalance().compareTo(new BigDecimal("100.00")));
        assertEquals(0, bob.getBalance().compareTo(new BigDecimal("50.00")));
    }

    @Test
    void negativeDepositIsRejected() {
        assertThrows(RuntimeException.class,
                () -> accountService.deposit(alice, new BigDecimal("-30.00")));
        assertEquals(0, alice.getBalance().compareTo(new BigDecimal("100.00")));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void negativeWithdrawalIsRejected() {
        assertThrows(RuntimeException.class,
                () -> accountService.withdraw(alice, new BigDecimal("-30.00")));
        assertEquals(0, alice.getBalance().compareTo(new BigDecimal("100.00")));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void zeroDepositIsRejected() {
        assertThrows(RuntimeException.class,
                () -> accountService.deposit(alice, BigDecimal.ZERO));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void loadUserByUsernameThrowsUsernameNotFoundForUnknownUser() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class,
                () -> accountService.loadUserByUsername("ghost"));
    }
}
