package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceBugHuntTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account alice;
    private Account bob;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        alice = accountService.registerAccount("alice", "pw");
        bob = accountService.registerAccount("bob", "pw");
        accountService.deposit(alice, new BigDecimal("100"));
    }

    private BigDecimal balanceOf(String username) {
        return accountService.findAccountByUsername(username).getBalance();
    }

    @Test
    void hasSufficientFunds_isTrueWhenBalanceEqualsAmount() {
        assertTrue(accountService.hasSufficientFunds(alice, new BigDecimal("100")));
    }

    @Test
    void withdraw_entireBalanceSucceeds() {
        accountService.withdraw(alice, new BigDecimal("100"));
        assertEquals(0, balanceOf("alice").compareTo(BigDecimal.ZERO));
    }

    @Test
    void transfer_entireBalanceSucceeds() {
        accountService.transferAmount(alice, "bob", new BigDecimal("100"));
        assertEquals(0, balanceOf("alice").compareTo(BigDecimal.ZERO));
        assertEquals(0, balanceOf("bob").compareTo(new BigDecimal("100")));
    }

    @Test
    void transactionHistoryRange_includesTheToDay() {
        LocalDate day = LocalDate.of(2026, 3, 10);
        transactionRepository.save(new Transaction(new BigDecimal("5"), "Deposit", day.atTime(12, 0), alice));

        List<Transaction> result = accountService.getTransactionHistory(alice, day, day);

        assertEquals(1, result.size());
    }

    @Test
    void transactionHistoryRange_includesLastMinuteOfToDay() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 10);
        transactionRepository.save(new Transaction(new BigDecimal("5"), "Deposit", to.atTime(23, 59, 59), alice));
        transactionRepository.save(new Transaction(new BigDecimal("6"), "Deposit", to.plusDays(1).atStartOfDay(), alice));

        List<Transaction> result = accountService.getTransactionHistory(alice, from, to);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getAmount().compareTo(new BigDecimal("5")));
    }

    @Test
    void transfer_negativeAmountIsRejected() {
        accountService.deposit(bob, new BigDecimal("500"));

        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(alice, "bob", new BigDecimal("-500")));

        assertEquals(0, balanceOf("alice").compareTo(new BigDecimal("100")));
        assertEquals(0, balanceOf("bob").compareTo(new BigDecimal("500")));
    }

    @Test
    void withdraw_negativeAmountIsRejected() {
        assertThrows(RuntimeException.class,
                () -> accountService.withdraw(alice, new BigDecimal("-1000")));
        assertEquals(0, balanceOf("alice").compareTo(new BigDecimal("100")));
    }

    @Test
    void deposit_negativeAmountIsRejected() {
        assertThrows(RuntimeException.class,
                () -> accountService.deposit(alice, new BigDecimal("-1000")));
        assertEquals(0, balanceOf("alice").compareTo(new BigDecimal("100")));
    }

    @Test
    void loadUserByUsername_unknownUserThrowsUsernameNotFoundException() {
        assertThrows(UsernameNotFoundException.class,
                () -> accountService.loadUserByUsername("nobody"));
    }
}
