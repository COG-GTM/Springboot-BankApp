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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private Account bob;

    @BeforeEach
    void setUp() {
        alice = account(1L, "alice", "100.00");
        bob = account(2L, "bob", "50.00");
        lenient().when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    }

    @Test
    void hasSufficientFundsIsTrueWhenBalanceEqualsAmount() {
        assertTrue(accountService.hasSufficientFunds(alice, new BigDecimal("100.00")));
    }

    @Test
    void withdrawOfEntireBalanceSucceeds() {
        accountService.withdraw(alice, new BigDecimal("100.00"));

        assertEquals(0, alice.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void transferOfEntireBalanceSucceeds() {
        accountService.transferAmount(alice, "bob", new BigDecimal("100.00"));

        assertEquals(0, alice.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, bob.getBalance().compareTo(new BigDecimal("150.00")));
    }

    @Test
    void transactionHistoryRangeIncludesTransactionsOnTheEndDay() {
        Transaction startDay = transaction(LocalDateTime.of(2026, 11, 1, 0, 0));
        Transaction endDay = transaction(LocalDateTime.of(2026, 11, 3, 14, 30));
        Transaction after = transaction(LocalDateTime.of(2026, 11, 4, 0, 0));
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(after, endDay, startDay));

        List<Transaction> result = accountService.getTransactionHistory(
                alice, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3));

        assertEquals(List.of(startDay, endDay), result);
    }

    @Test
    void transactionHistoryRangeForASingleDayReturnsThatDaysTransactions() {
        Transaction sameDay = transaction(LocalDateTime.of(2026, 11, 2, 9, 15));
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(sameDay));

        List<Transaction> result = accountService.getTransactionHistory(
                alice, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2));

        assertEquals(List.of(sameDay), result);
    }

    @Test
    void loadUserByUsernameThrowsUsernameNotFoundExceptionForUnknownUser() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> accountService.loadUserByUsername("ghost"));
    }

    @Test
    void transferRejectsNegativeAmount() {
        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(alice, "bob", new BigDecimal("-30.00")));

        assertEquals(0, alice.getBalance().compareTo(new BigDecimal("100.00")));
        assertEquals(0, bob.getBalance().compareTo(new BigDecimal("50.00")));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdrawRejectsNegativeAmount() {
        assertThrows(RuntimeException.class, () -> accountService.withdraw(alice, new BigDecimal("-30.00")));

        assertEquals(0, alice.getBalance().compareTo(new BigDecimal("100.00")));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void depositRejectsNegativeAmount() {
        assertThrows(RuntimeException.class, () -> accountService.deposit(alice, new BigDecimal("-30.00")));

        assertEquals(0, alice.getBalance().compareTo(new BigDecimal("100.00")));
        verify(accountRepository, never()).save(any());
    }

    private static Account account(Long id, String username, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    private Transaction transaction(LocalDateTime timestamp) {
        return new Transaction(BigDecimal.TEN, "Deposit", timestamp, alice);
    }
}
