package com.example.bankapp;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void registerAccountCreatesAccountWithZeroBalanceAndEncodedPassword() {
        Account account = accountService.registerAccount("alice", "secret");

        assertEquals("alice", account.getUsername());
        assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
        assertNotEquals("secret", account.getPassword());
        assertTrue(passwordEncoder.matches("secret", account.getPassword()));
        assertTrue(accountRepository.findByUsername("alice").isPresent());
    }

    @Test
    void registerAccountWithDuplicateUsernameThrows() {
        accountService.registerAccount("bob", "pw1");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("bob", "pw2"));
        assertEquals("Username already exists", ex.getMessage());
    }

    @Test
    void depositIncreasesBalanceAndRecordsTransaction() {
        Account account = accountService.registerAccount("carol", "pw");

        accountService.deposit(account, new BigDecimal("100.00"));

        Account updated = accountService.findAccountByUsername("carol");
        assertEquals(0, new BigDecimal("100.00").compareTo(updated.getBalance()));

        List<Transaction> transactions = accountService.getTransactionHistory(updated);
        assertEquals(1, transactions.size());
        assertEquals("Deposit", transactions.get(0).getType());
        assertEquals(0, new BigDecimal("100.00").compareTo(transactions.get(0).getAmount()));
    }

    @Test
    void withdrawDecreasesBalanceAndRecordsTransaction() {
        Account account = accountService.registerAccount("dave", "pw");
        accountService.deposit(account, new BigDecimal("100.00"));

        Account funded = accountService.findAccountByUsername("dave");
        accountService.withdraw(funded, new BigDecimal("40.00"));

        Account updated = accountService.findAccountByUsername("dave");
        assertEquals(0, new BigDecimal("60.00").compareTo(updated.getBalance()));

        List<Transaction> transactions = accountService.getTransactionHistory(updated);
        assertTrue(transactions.stream().anyMatch(t -> "Withdrawal".equals(t.getType())
                && new BigDecimal("40.00").compareTo(t.getAmount()) == 0));
    }

    @Test
    void withdrawWithInsufficientFundsThrows() {
        Account account = accountService.registerAccount("erin", "pw");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("10.00")));
        assertEquals("Insufficient funds", ex.getMessage());
    }

    @Test
    void transferAmountMovesFundsAndRecordsTwoTransactions() {
        Account sender = accountService.registerAccount("frank", "pw");
        accountService.registerAccount("grace", "pw");
        accountService.deposit(sender, new BigDecimal("200.00"));

        Account funded = accountService.findAccountByUsername("frank");
        accountService.transferAmount(funded, "grace", new BigDecimal("75.00"));

        Account from = accountService.findAccountByUsername("frank");
        Account to = accountService.findAccountByUsername("grace");
        assertEquals(0, new BigDecimal("125.00").compareTo(from.getBalance()));
        assertEquals(0, new BigDecimal("75.00").compareTo(to.getBalance()));

        assertTrue(accountService.getTransactionHistory(from).stream()
                .anyMatch(t -> t.getType().startsWith("Transfer Out")));
        assertTrue(accountService.getTransactionHistory(to).stream()
                .anyMatch(t -> t.getType().startsWith("Transfer In")));
    }

    @Test
    void transferAmountWithInsufficientFundsThrows() {
        Account sender = accountService.registerAccount("heidi", "pw");
        accountService.registerAccount("ivan", "pw");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "ivan", new BigDecimal("50.00")));
        assertEquals("Insufficient funds", ex.getMessage());
    }

    @Test
    void getTransactionHistoryReturnsTransactionsForCorrectAccount() {
        Account first = accountService.registerAccount("judy", "pw");
        Account second = accountService.registerAccount("mallory", "pw");
        accountService.deposit(first, new BigDecimal("10.00"));
        accountService.deposit(second, new BigDecimal("20.00"));

        Account judy = accountService.findAccountByUsername("judy");
        List<Transaction> history = accountService.getTransactionHistory(judy);

        assertEquals(1, history.size());
        assertEquals(judy.getId(), history.get(0).getAccount().getId());
        assertEquals(0, new BigDecimal("10.00").compareTo(history.get(0).getAmount()));
    }
}
