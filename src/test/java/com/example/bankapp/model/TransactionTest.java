package com.example.bankapp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testNoArgsConstructor() {
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

        Transaction transaction = new Transaction(new BigDecimal("100.00"), "Deposit", now, account);

        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals("Deposit", transaction.getType());
        assertEquals(now, transaction.getTimestamp());
        assertSame(account, transaction.getAccount());
    }

    @Test
    void testSetAndGetId() {
        Transaction transaction = new Transaction();
        transaction.setId(10L);
        assertEquals(10L, transaction.getId());
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
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        transaction.setTimestamp(now);
        assertEquals(now, transaction.getTimestamp());
    }

    @Test
    void testSetAndGetAccount() {
        Transaction transaction = new Transaction();
        Account account = new Account();
        account.setUsername("testuser");
        transaction.setAccount(account);
        assertEquals("testuser", transaction.getAccount().getUsername());
    }

    @Test
    void testSetIdToNull() {
        Transaction transaction = new Transaction();
        transaction.setId(5L);
        assertEquals(5L, transaction.getId());
        transaction.setId(null);
        assertNull(transaction.getId());
    }

    @Test
    void testSetAmountToZero() {
        Transaction transaction = new Transaction();
        transaction.setAmount(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, transaction.getAmount());
    }

    @Test
    void testSetTypeToNull() {
        Transaction transaction = new Transaction();
        transaction.setType(null);
        assertNull(transaction.getType());
    }

    @Test
    void testSetTimestampToNull() {
        Transaction transaction = new Transaction();
        transaction.setTimestamp(null);
        assertNull(transaction.getTimestamp());
    }

    @Test
    void testSetAccountToNull() {
        Transaction transaction = new Transaction();
        transaction.setAccount(null);
        assertNull(transaction.getAccount());
    }

    @Test
    void testConstructorWithNullValues() {
        Transaction transaction = new Transaction(null, null, null, null);
        assertNull(transaction.getAmount());
        assertNull(transaction.getType());
        assertNull(transaction.getTimestamp());
        assertNull(transaction.getAccount());
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
}
