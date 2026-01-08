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
        
        LocalDateTime timestamp = LocalDateTime.now();
        BigDecimal amount = new BigDecimal("100.00");
        
        Transaction transaction = new Transaction(amount, "Deposit", timestamp, account);
        
        assertEquals(amount, transaction.getAmount());
        assertEquals("Deposit", transaction.getType());
        assertEquals(timestamp, transaction.getTimestamp());
        assertEquals(account, transaction.getAccount());
    }

    @Test
    void testSettersAndGetters() {
        Transaction transaction = new Transaction();
        Account account = new Account();
        account.setId(1L);
        
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30);
        
        transaction.setId(1L);
        transaction.setAmount(new BigDecimal("250.50"));
        transaction.setType("Withdrawal");
        transaction.setTimestamp(timestamp);
        transaction.setAccount(account);
        
        assertEquals(1L, transaction.getId());
        assertEquals(new BigDecimal("250.50"), transaction.getAmount());
        assertEquals("Withdrawal", transaction.getType());
        assertEquals(timestamp, transaction.getTimestamp());
        assertEquals(account, transaction.getAccount());
    }

    @Test
    void testDifferentTransactionTypes() {
        Transaction deposit = new Transaction();
        deposit.setType("Deposit");
        assertEquals("Deposit", deposit.getType());
        
        Transaction withdrawal = new Transaction();
        withdrawal.setType("Withdrawal");
        assertEquals("Withdrawal", withdrawal.getType());
        
        Transaction transferOut = new Transaction();
        transferOut.setType("Transfer Out to user2");
        assertEquals("Transfer Out to user2", transferOut.getType());
        
        Transaction transferIn = new Transaction();
        transferIn.setType("Transfer In from user1");
        assertEquals("Transfer In from user1", transferIn.getType());
    }

    @Test
    void testAmountPrecision() {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("1234567.89"));
        assertEquals(new BigDecimal("1234567.89"), transaction.getAmount());
    }
}
