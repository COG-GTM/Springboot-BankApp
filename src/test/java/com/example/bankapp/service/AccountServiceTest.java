package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void cleanUp() {
        accountRepository.deleteAll();
    }

    @Test
    void registerAccountPersistsEncodedPasswordAndZeroBalance() {
        Account account = accountService.registerAccount("alice", "secret");

        assertNotNull(account.getId());
        assertEquals("alice", account.getUsername());
        assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
        assertTrue(accountRepository.findByUsername("alice").isPresent());
        assertNotEquals("secret", account.getPassword());
    }

    @Test
    void registerAccountRejectsDuplicateUsername() {
        accountService.registerAccount("bob", "pw");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("bob", "pw2"));
        assertEquals("Username already exists", ex.getMessage());
    }

    @Test
    void depositIncreasesBalanceAndRecordsTransaction() {
        Account account = accountService.registerAccount("carol", "pw");

        accountService.deposit(account, new BigDecimal("100.00"));

        Account reloaded = accountService.findAccountByUsername("carol");
        assertEquals(0, new BigDecimal("100.00").compareTo(reloaded.getBalance()));

        List<Transaction> history = accountService.getTransactionHistory(reloaded);
        assertEquals(1, history.size());
        assertEquals("Deposit", history.get(0).getType());
        assertEquals(0, new BigDecimal("100.00").compareTo(history.get(0).getAmount()));
    }

    @Test
    void withdrawDecreasesBalanceAndRecordsTransaction() {
        Account account = accountService.registerAccount("dave", "pw");
        accountService.deposit(account, new BigDecimal("100.00"));

        accountService.withdraw(account, new BigDecimal("40.00"));

        Account reloaded = accountService.findAccountByUsername("dave");
        assertEquals(0, new BigDecimal("60.00").compareTo(reloaded.getBalance()));
        assertTrue(accountService.getTransactionHistory(reloaded).stream()
                .anyMatch(t -> t.getType().equals("Withdrawal")));
    }

    @Test
    void withdrawRejectsInsufficientFunds() {
        Account account = accountService.registerAccount("erin", "pw");
        accountService.deposit(account, new BigDecimal("10.00"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("50.00")));
        assertEquals("Insufficient funds", ex.getMessage());

        assertEquals(0, new BigDecimal("10.00")
                .compareTo(accountService.findAccountByUsername("erin").getBalance()));
    }

    @Test
    void transferMovesFundsAndRecordsBothSides() {
        Account from = accountService.registerAccount("frank", "pw");
        accountService.registerAccount("grace", "pw");
        accountService.deposit(from, new BigDecimal("100.00"));

        accountService.transferAmount(from, "grace", new BigDecimal("30.00"));

        Account sender = accountService.findAccountByUsername("frank");
        Account recipient = accountService.findAccountByUsername("grace");
        assertEquals(0, new BigDecimal("70.00").compareTo(sender.getBalance()));
        assertEquals(0, new BigDecimal("30.00").compareTo(recipient.getBalance()));

        assertTrue(accountService.getTransactionHistory(sender).stream()
                .anyMatch(t -> t.getType().equals("Transfer Out to grace")));
        assertTrue(accountService.getTransactionHistory(recipient).stream()
                .anyMatch(t -> t.getType().equals("Transfer In from frank")));
    }

    @Test
    void transferRejectsInsufficientFunds() {
        Account from = accountService.registerAccount("heidi", "pw");
        accountService.registerAccount("ivan", "pw");
        accountService.deposit(from, new BigDecimal("10.00"));

        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(from, "ivan", new BigDecimal("50.00")));
    }

    @Test
    void transferRejectsUnknownRecipient() {
        Account from = accountService.registerAccount("judy", "pw");
        accountService.deposit(from, new BigDecimal("100.00"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(from, "nobody", new BigDecimal("10.00")));
        assertEquals("Recipient account not found", ex.getMessage());
    }

    @Test
    void findAccountByUsernameThrowsWhenMissing() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("ghost"));
        assertEquals("Account not found", ex.getMessage());
    }

    @Test
    void loadUserByUsernameReturnsUserDetailsWithAuthority() {
        accountService.registerAccount("mallory", "pw");

        UserDetails details = accountService.loadUserByUsername("mallory");
        assertEquals("mallory", details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("USER")));
    }

    @Test
    void loadUserByUsernameThrowsForUnknownUser() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("unknown"));
        assertEquals("Account not found", ex.getMessage());
    }
}
