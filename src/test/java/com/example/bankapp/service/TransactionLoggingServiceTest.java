package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.example.bankapp.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TransactionLoggingServiceTest {

    @Autowired
    private TransactionLoggingService transactionLoggingService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        testAccount = new Account();
        testAccount.setUsername("asynctestuser");
        testAccount.setPassword(passwordEncoder.encode("password"));
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount = accountRepository.save(testAccount);
    }

    @Test
    void logDepositAsync_shouldReturnCompletableFuture() throws Exception {
        CompletableFuture<Transaction> future = transactionLoggingService.logDepositAsync(
                testAccount, new BigDecimal("100.00"));

        Transaction result = future.get();

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Deposit", result.getType());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
    }

    @Test
    void logWithdrawalAsync_shouldPersistTransaction() throws Exception {
        CompletableFuture<Transaction> future = transactionLoggingService.logWithdrawalAsync(
                testAccount, new BigDecimal("50.00"));

        Transaction result = future.get();

        assertNotNull(result);
        assertEquals("Withdrawal", result.getType());
        assertEquals(new BigDecimal("50.00"), result.getAmount());
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
    }

    @Test
    void logTransferAsync_shouldPersistWithCorrectType() throws Exception {
        CompletableFuture<Transaction> future = transactionLoggingService.logTransferAsync(
                testAccount, new BigDecimal("200.00"), "Transfer Out to recipient");

        Transaction result = future.get();

        assertNotNull(result);
        assertEquals("Transfer Out to recipient", result.getType());
        assertEquals(new BigDecimal("200.00"), result.getAmount());
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
    }

    @Test
    void logDepositAsync_shouldExecuteOnDifferentThread() throws Exception {
        String callerThread = Thread.currentThread().getName();

        CompletableFuture<Transaction> future = transactionLoggingService.logDepositAsync(
                testAccount, new BigDecimal("100.00"));

        future.get();

        assertNotEquals(callerThread, "payment-async-",
                "Async method should run on a payment-async thread, not the caller thread");
    }
}
