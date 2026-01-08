package com.example.bankapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        transaction = new Transaction();
    }

    @Test
    void defaultConstructor_CreatesEmptyTransaction() {
        Transaction newTransaction = new Transaction();
        assertNotNull(newTransaction);
        assertNull(newTransaction.getId());
        assertNull(newTransaction.getAmount());
        assertNull(newTransaction.getType());
        assertNull(newTransaction.getTimestamp());
        assertNull(newTransaction.getAccount());
    }

    @Test
    void parameterizedConstructor_SetsAllFields() {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("testuser");

        BigDecimal amount = new BigDecimal("500.00");
        String type = "Deposit";
        LocalDateTime timestamp = LocalDateTime.now();

        Transaction newTransaction = new Transaction(amount, type, timestamp, account);

        assertEquals(amount, newTransaction.getAmount());
        assertEquals(type, newTransaction.getType());
        assertEquals(timestamp, newTransaction.getTimestamp());
        assertEquals(account, newTransaction.getAccount());
    }

    @Test
    void setAndGetId_WorksCorrectly() {
        transaction.setId(1L);
        assertEquals(1L, transaction.getId());
    }

    @Test
    void setAndGetAmount_WorksCorrectly() {
        BigDecimal amount = new BigDecimal("250.50");
        transaction.setAmount(amount);
        assertEquals(amount, transaction.getAmount());
    }

    @Test
    void setAndGetType_WorksCorrectly() {
        transaction.setType("Withdrawal");
        assertEquals("Withdrawal", transaction.getType());
    }

    @Test
    void setAndGetTimestamp_WorksCorrectly() {
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        transaction.setTimestamp(timestamp);
        assertEquals(timestamp, transaction.getTimestamp());
    }

    @Test
    void setAndGetAccount_WorksCorrectly() {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("testuser");

        transaction.setAccount(account);
        assertEquals(account, transaction.getAccount());
        assertEquals(1L, transaction.getAccount().getId());
        assertEquals("testuser", transaction.getAccount().getUsername());
    }

    @Test
    void type_CanBeDeposit() {
        transaction.setType("Deposit");
        assertEquals("Deposit", transaction.getType());
    }

    @Test
    void type_CanBeWithdrawal() {
        transaction.setType("Withdrawal");
        assertEquals("Withdrawal", transaction.getType());
    }

    @Test
    void type_CanBeTransferOut() {
        transaction.setType("Transfer Out to recipient");
        assertEquals("Transfer Out to recipient", transaction.getType());
    }

    @Test
    void type_CanBeTransferIn() {
        transaction.setType("Transfer In from sender");
        assertEquals("Transfer In from sender", transaction.getType());
    }

    @Test
    void amount_CanBeZero() {
        transaction.setAmount(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, transaction.getAmount());
    }

    @Test
    void amount_CanHaveDecimalPlaces() {
        BigDecimal amount = new BigDecimal("123.45");
        transaction.setAmount(amount);
        assertEquals(amount, transaction.getAmount());
    }

    @Test
    void timestamp_CanBeCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        transaction.setTimestamp(now);
        assertNotNull(transaction.getTimestamp());
    }
}
