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
        lenient().when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Account account(Long id, String username, String balance) {
        Account a = new Account();
        a.setId(id);
        a.setUsername(username);
        a.setBalance(new BigDecimal(balance));
        return a;
    }

    @Test
    void hasSufficientFundsIsTrueWhenBalanceEqualsAmount() {
        assertTrue(accountService.hasSufficientFunds(alice, new BigDecimal("100.00")));
    }

    @Test
    void withdrawEntireBalanceSucceeds() {
        accountService.withdraw(alice, new BigDecimal("100.00"));
        assertEquals(0, alice.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void transferEntireBalanceSucceeds() {
        accountService.transferAmount(alice, "bob", new BigDecimal("100.00"));
        assertEquals(0, alice.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, bob.getBalance().compareTo(new BigDecimal("150.00")));
    }

    @Test
    void transactionHistoryRangeIncludesTransactionsOnEndDate() {
        Transaction onEndDay = new Transaction(BigDecimal.TEN, "Deposit",
                LocalDateTime.of(2026, 11, 20, 14, 30), alice);
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(onEndDay));

        List<Transaction> result = accountService.getTransactionHistory(alice,
                LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 20));

        assertEquals(List.of(onEndDay), result);
    }

    @Test
    void transactionHistorySingleDayRangeIncludesThatDay() {
        Transaction sameDay = new Transaction(BigDecimal.TEN, "Deposit",
                LocalDateTime.of(2026, 11, 20, 9, 0), alice);
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(sameDay));

        LocalDate day = LocalDate.of(2026, 11, 20);
        assertEquals(List.of(sameDay), accountService.getTransactionHistory(alice, day, day));
    }

    @Test
    void loadUserByUsernameThrowsUsernameNotFoundForUnknownUser() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> accountService.loadUserByUsername("ghost"));
    }

    @Test
    void transferRejectsNegativeAmount() {
        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(alice, "bob", new BigDecimal("-30.00")));
        assertEquals(0, alice.getBalance().compareTo(new BigDecimal("100.00")));
        assertEquals(0, bob.getBalance().compareTo(new BigDecimal("50.00")));
    }

    @Test
    void withdrawRejectsNegativeAmount() {
        assertThrows(RuntimeException.class,
                () -> accountService.withdraw(alice, new BigDecimal("-30.00")));
        assertEquals(0, alice.getBalance().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void depositRejectsNegativeAmount() {
        assertThrows(RuntimeException.class,
                () -> accountService.deposit(alice, new BigDecimal("-30.00")));
        assertEquals(0, alice.getBalance().compareTo(new BigDecimal("100.00")));
    }
}
