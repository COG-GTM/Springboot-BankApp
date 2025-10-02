package com.example.bankapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionModelTest {

    private Transaction transaction;
    private Account account;
    private LocalDateTime timestamp;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        
        timestamp = LocalDateTime.now();
        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setType("Deposit");
        transaction.setTimestamp(timestamp);
        transaction.setAccount(account);
    }

    @Test
    void testTransactionCreation() {
        assertNotNull(transaction);
        assertEquals(1L, transaction.getId());
        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals("Deposit", transaction.getType());
        assertEquals(timestamp, transaction.getTimestamp());
        assertEquals(account, transaction.getAccount());
    }

    @Test
    void testTransactionConstructorWithParameters() {
        LocalDateTime testTime = LocalDateTime.of(2024, 1, 1, 12, 0);
        Transaction newTransaction = new Transaction(
            new BigDecimal("200.00"),
            "Withdrawal",
            testTime,
            account
        );

        assertEquals(new BigDecimal("200.00"), newTransaction.getAmount());
        assertEquals("Withdrawal", newTransaction.getType());
        assertEquals(testTime, newTransaction.getTimestamp());
        assertEquals(account, newTransaction.getAccount());
    }

    @Test
    void testDepositTransaction() {
        transaction.setType("Deposit");
        transaction.setAmount(new BigDecimal("250.00"));

        assertEquals("Deposit", transaction.getType());
        assertEquals(new BigDecimal("250.00"), transaction.getAmount());
    }

    @Test
    void testWithdrawalTransaction() {
        transaction.setType("Withdrawal");
        transaction.setAmount(new BigDecimal("75.00"));

        assertEquals("Withdrawal", transaction.getType());
        assertEquals(new BigDecimal("75.00"), transaction.getAmount());
    }

    @Test
    void testTransferTransaction() {
        transaction.setType("Transfer");
        transaction.setAmount(new BigDecimal("300.00"));

        assertEquals("Transfer", transaction.getType());
        assertEquals(new BigDecimal("300.00"), transaction.getAmount());
    }
}
