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
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction(new BigDecimal("100.00"), "Deposit", now, account);

        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals("Deposit", transaction.getType());
        assertEquals(now, transaction.getTimestamp());
        assertEquals(account, transaction.getAccount());
    }

    @Test
    void testGetAndSetId() {
        Transaction transaction = new Transaction();
        assertNull(transaction.getId());

        transaction.setId(1L);
        assertEquals(1L, transaction.getId());

        transaction.setId(null);
        assertNull(transaction.getId());
    }

    @Test
    void testGetAndSetAmount() {
        Transaction transaction = new Transaction();
        assertNull(transaction.getAmount());

        transaction.setAmount(new BigDecimal("250.75"));
        assertEquals(new BigDecimal("250.75"), transaction.getAmount());

        transaction.setAmount(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, transaction.getAmount());

        transaction.setAmount(null);
        assertNull(transaction.getAmount());
    }

    @Test
    void testGetAndSetType() {
        Transaction transaction = new Transaction();
        assertNull(transaction.getType());

        transaction.setType("Deposit");
        assertEquals("Deposit", transaction.getType());

        transaction.setType("Withdrawal");
        assertEquals("Withdrawal", transaction.getType());

        transaction.setType("Transfer Out to user2");
        assertEquals("Transfer Out to user2", transaction.getType());

        transaction.setType("");
        assertEquals("", transaction.getType());

        transaction.setType(null);
        assertNull(transaction.getType());
    }

    @Test
    void testGetAndSetTimestamp() {
        Transaction transaction = new Transaction();
        assertNull(transaction.getTimestamp());

        LocalDateTime now = LocalDateTime.now();
        transaction.setTimestamp(now);
        assertEquals(now, transaction.getTimestamp());

        LocalDateTime past = LocalDateTime.of(2023, 1, 1, 0, 0);
        transaction.setTimestamp(past);
        assertEquals(past, transaction.getTimestamp());

        transaction.setTimestamp(null);
        assertNull(transaction.getTimestamp());
    }

    @Test
    void testGetAndSetAccount() {
        Transaction transaction = new Transaction();
        assertNull(transaction.getAccount());

        Account account = new Account();
        account.setId(5L);
        account.setUsername("user5");
        transaction.setAccount(account);
        assertEquals(account, transaction.getAccount());
        assertEquals("user5", transaction.getAccount().getUsername());

        transaction.setAccount(null);
        assertNull(transaction.getAccount());
    }

    @Test
    void testParameterizedConstructorWithNullValues() {
        Transaction transaction = new Transaction(null, null, null, null);

        assertNull(transaction.getAmount());
        assertNull(transaction.getType());
        assertNull(transaction.getTimestamp());
        assertNull(transaction.getAccount());
    }

    @Test
    void testTransactionTypes() {
        Transaction deposit = new Transaction(new BigDecimal("50"), "Deposit", LocalDateTime.now(), new Account());
        assertEquals("Deposit", deposit.getType());

        Transaction withdrawal = new Transaction(new BigDecimal("30"), "Withdrawal", LocalDateTime.now(), new Account());
        assertEquals("Withdrawal", withdrawal.getType());

        Transaction transferOut = new Transaction(new BigDecimal("20"), "Transfer Out to user2", LocalDateTime.now(), new Account());
        assertEquals("Transfer Out to user2", transferOut.getType());

        Transaction transferIn = new Transaction(new BigDecimal("20"), "Transfer In from user1", LocalDateTime.now(), new Account());
        assertEquals("Transfer In from user1", transferIn.getType());
    }
}
