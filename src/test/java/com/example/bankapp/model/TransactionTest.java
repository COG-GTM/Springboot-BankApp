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
    }

    @Test
    void parameterizedConstructor_setsAllFields() {
        Account account = new Account();
        LocalDateTime timestamp = LocalDateTime.of(2025, 1, 15, 10, 30);

        Transaction transaction = new Transaction(new BigDecimal("50.00"), "Deposit", timestamp, account);

        assertEquals(new BigDecimal("50.00"), transaction.getAmount());
        assertEquals("Deposit", transaction.getType());
        assertEquals(timestamp, transaction.getTimestamp());
        assertEquals(account, transaction.getAccount());
    }

    @Test
    void gettersAndSetters_id() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        assertEquals(1L, transaction.getId());
    }

    @Test
    void gettersAndSetters_amount() {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("75.25"));
        assertEquals(new BigDecimal("75.25"), transaction.getAmount());
    }

    @Test
    void gettersAndSetters_type() {
        Transaction transaction = new Transaction();
        transaction.setType("Withdrawal");
        assertEquals("Withdrawal", transaction.getType());
    }

    @Test
    void gettersAndSetters_timestamp() {
        Transaction transaction = new Transaction();
        LocalDateTime now = LocalDateTime.now();
        transaction.setTimestamp(now);
        assertEquals(now, transaction.getTimestamp());
    }

    @Test
    void gettersAndSetters_account() {
        Transaction transaction = new Transaction();
        Account account = new Account();
        account.setUsername("testuser");
        transaction.setAccount(account);
        assertEquals(account, transaction.getAccount());
    }
}
