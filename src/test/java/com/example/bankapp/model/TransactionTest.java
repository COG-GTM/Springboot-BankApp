package com.example.bankapp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testDefaultConstructor() {
        Transaction transaction = new Transaction();
        assertNull(transaction.getId());
        assertNull(transaction.getAmount());
        assertNull(transaction.getType());
        assertNull(transaction.getTimestamp());
        assertNull(transaction.getAccount());
    }

    @Test
    void testParameterizedConstructor() {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        
        BigDecimal amount = new BigDecimal("100.00");
        String type = "Deposit";
        LocalDateTime timestamp = LocalDateTime.now();
        
        Transaction transaction = new Transaction(amount, type, timestamp, account);
        
        assertEquals(amount, transaction.getAmount());
        assertEquals(type, transaction.getType());
        assertEquals(timestamp, transaction.getTimestamp());
        assertEquals(account, transaction.getAccount());
    }

    @Test
    void testSetAndGetId() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        assertEquals(1L, transaction.getId());
    }

    @Test
    void testSetAndGetAmount() {
        Transaction transaction = new Transaction();
        BigDecimal amount = new BigDecimal("250.75");
        transaction.setAmount(amount);
        assertEquals(amount, transaction.getAmount());
    }

    @Test
    void testSetAndGetType() {
        Transaction transaction = new Transaction();
        transaction.setType("Withdrawal");
        assertEquals("Withdrawal", transaction.getType());
    }

    @Test
    void testSetAndGetTimestamp() {
        Transaction transaction = new Transaction();
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        transaction.setTimestamp(timestamp);
        assertEquals(timestamp, transaction.getTimestamp());
    }

    @Test
    void testSetAndGetAccount() {
        Transaction transaction = new Transaction();
        Account account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        transaction.setAccount(account);
        assertEquals(account, transaction.getAccount());
        assertEquals("testuser", transaction.getAccount().getUsername());
    }

    @Test
    void testDepositTransaction() {
        Account account = new Account();
        account.setUsername("depositor");
        
        Transaction transaction = new Transaction(
            new BigDecimal("500.00"),
            "Deposit",
            LocalDateTime.now(),
            account
        );
        
        assertEquals("Deposit", transaction.getType());
        assertEquals(new BigDecimal("500.00"), transaction.getAmount());
    }

    @Test
    void testWithdrawalTransaction() {
        Account account = new Account();
        account.setUsername("withdrawer");
        
        Transaction transaction = new Transaction(
            new BigDecimal("200.00"),
            "Withdrawal",
            LocalDateTime.now(),
            account
        );
        
        assertEquals("Withdrawal", transaction.getType());
        assertEquals(new BigDecimal("200.00"), transaction.getAmount());
    }

    @Test
    void testTransferTransaction() {
        Account account = new Account();
        account.setUsername("sender");
        
        Transaction transaction = new Transaction(
            new BigDecimal("300.00"),
            "Transfer Out to recipient",
            LocalDateTime.now(),
            account
        );
        
        assertEquals("Transfer Out to recipient", transaction.getType());
        assertEquals(new BigDecimal("300.00"), transaction.getAmount());
    }
}
