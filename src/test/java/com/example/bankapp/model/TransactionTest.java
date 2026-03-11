package com.example.bankapp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void defaultConstructor_shouldCreateEmptyTransaction() {
        Transaction transaction = new Transaction();

        assertNull(transaction.getId());
        assertNull(transaction.getAmount());
        assertNull(transaction.getType());
        assertNull(transaction.getTimestamp());
        assertNull(transaction.getAccount());
    }

    @Test
    void parameterizedConstructor_shouldSetAllFields() {
        Account account = new Account();
        account.setUsername("testuser");
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction(new BigDecimal("100"), "Deposit", now, account);

        assertEquals(new BigDecimal("100"), transaction.getAmount());
        assertEquals("Deposit", transaction.getType());
        assertEquals(now, transaction.getTimestamp());
        assertEquals(account, transaction.getAccount());
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        Transaction transaction = new Transaction();

        transaction.setId(1L);
        assertEquals(1L, transaction.getId());

        transaction.setAmount(new BigDecimal("200"));
        assertEquals(new BigDecimal("200"), transaction.getAmount());

        transaction.setType("Withdrawal");
        assertEquals("Withdrawal", transaction.getType());

        LocalDateTime now = LocalDateTime.now();
        transaction.setTimestamp(now);
        assertEquals(now, transaction.getTimestamp());

        Account account = new Account();
        transaction.setAccount(account);
        assertEquals(account, transaction.getAccount());
    }
}
