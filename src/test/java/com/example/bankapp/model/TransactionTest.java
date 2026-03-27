package com.example.bankapp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void noArgConstructor_createsInstance() {
        Transaction transaction = new Transaction();
        assertNotNull(transaction);
        assertNull(transaction.getId());
        assertNull(transaction.getAmount());
        assertNull(transaction.getType());
        assertNull(transaction.getTimestamp());
        assertNull(transaction.getAccount());
    }

    @Test
    void parameterizedConstructor_setsAllFields() {
        Account account = new Account();
        account.setUsername("testuser");
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction(
                new BigDecimal("100.00"), "Deposit", now, account);

        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals("Deposit", transaction.getType());
        assertEquals(now, transaction.getTimestamp());
        assertEquals(account, transaction.getAccount());
    }

    @Test
    void settersAndGetters() {
        Transaction transaction = new Transaction();

        transaction.setId(1L);
        assertEquals(1L, transaction.getId());

        transaction.setAmount(new BigDecimal("75.50"));
        assertEquals(new BigDecimal("75.50"), transaction.getAmount());

        transaction.setType("Withdrawal");
        assertEquals("Withdrawal", transaction.getType());

        LocalDateTime now = LocalDateTime.now();
        transaction.setTimestamp(now);
        assertEquals(now, transaction.getTimestamp());

        Account account = new Account();
        account.setUsername("owner");
        transaction.setAccount(account);
        assertEquals(account, transaction.getAccount());
    }
}
