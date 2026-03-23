package com.example.bankapp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testNoArgConstructor() {
        Transaction transaction = new Transaction();
        assertNull(transaction.getId());
        assertNull(transaction.getAmount());
        assertNull(transaction.getType());
        assertNull(transaction.getTimestamp());
        assertNull(transaction.getAccount());
    }

    @Test
    void testAllArgsConstructor() {
        Account account = new Account();
        account.setId(1L);
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction(BigDecimal.TEN, "Deposit", now, account);

        assertEquals(BigDecimal.TEN, transaction.getAmount());
        assertEquals("Deposit", transaction.getType());
        assertEquals(now, transaction.getTimestamp());
        assertEquals(account, transaction.getAccount());
    }

    @Test
    void testSetAndGetId() {
        Transaction transaction = new Transaction();
        transaction.setId(5L);
        assertEquals(5L, transaction.getId());
    }

    @Test
    void testSetAndGetAmount() {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("250.75"));
        assertEquals(new BigDecimal("250.75"), transaction.getAmount());
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
        LocalDateTime now = LocalDateTime.now();
        transaction.setTimestamp(now);
        assertEquals(now, transaction.getTimestamp());
    }

    @Test
    void testSetAndGetAccount() {
        Transaction transaction = new Transaction();
        Account account = new Account();
        account.setUsername("user1");
        transaction.setAccount(account);
        assertEquals("user1", transaction.getAccount().getUsername());
    }

    @Test
    void testNullValues() {
        Transaction transaction = new Transaction(null, null, null, null);
        assertNull(transaction.getAmount());
        assertNull(transaction.getType());
        assertNull(transaction.getTimestamp());
        assertNull(transaction.getAccount());
    }
}
