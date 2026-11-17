package com.example.bankapp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TransactionTest {

    @Test
    void allArgsConstructorSetsEveryField() {
        BigDecimal amount = new BigDecimal("250.75");
        String type = "DEPOSIT";
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
        Account account = new Account();
        account.setUsername("alice");

        Transaction transaction = new Transaction(amount, type, timestamp, account);

        assertEquals(amount, transaction.getAmount());
        assertEquals(type, transaction.getType());
        assertEquals(timestamp, transaction.getTimestamp());
        assertSame(account, transaction.getAccount());
    }

    @Test
    void noArgsConstructorLeavesFieldsNull() {
        Transaction transaction = new Transaction();

        assertNull(transaction.getId());
        assertNull(transaction.getAmount());
        assertNull(transaction.getType());
        assertNull(transaction.getTimestamp());
        assertNull(transaction.getAccount());
    }

    @Test
    void setIdUpdatesField() {
        Transaction transaction = new Transaction();

        transaction.setId(42L);

        assertEquals(42L, transaction.getId());
    }

    @Test
    void setAmountUpdatesField() {
        Transaction transaction = new Transaction();
        BigDecimal amount = new BigDecimal("1000.00");

        transaction.setAmount(amount);

        assertEquals(amount, transaction.getAmount());
    }

    @Test
    void setTypeUpdatesField() {
        Transaction transaction = new Transaction();

        transaction.setType("WITHDRAWAL");

        assertEquals("WITHDRAWAL", transaction.getType());
    }

    @Test
    void setTimestampUpdatesField() {
        Transaction transaction = new Transaction();
        LocalDateTime timestamp = LocalDateTime.of(2023, 12, 31, 23, 59, 59);

        transaction.setTimestamp(timestamp);

        assertEquals(timestamp, transaction.getTimestamp());
    }

    @Test
    void setAccountUpdatesField() {
        Transaction transaction = new Transaction();
        Account account = new Account();
        account.setUsername("bob");
        account.setBalance(new BigDecimal("500.00"));

        transaction.setAccount(account);

        assertSame(account, transaction.getAccount());
        assertEquals("bob", transaction.getAccount().getUsername());
        assertEquals(new BigDecimal("500.00"), transaction.getAccount().getBalance());
    }

    @Test
    void gettersReflectUpdatedValuesAfterMutation() {
        Transaction transaction = new Transaction(
                new BigDecimal("10.00"), "DEPOSIT",
                LocalDateTime.of(2024, 5, 1, 9, 0), new Account());

        BigDecimal newAmount = new BigDecimal("99.99");
        LocalDateTime newTimestamp = LocalDateTime.of(2025, 6, 16, 14, 32);
        Account newAccount = new Account();
        newAccount.setUsername("carol");

        transaction.setAmount(newAmount);
        transaction.setType("TRANSFER");
        transaction.setTimestamp(newTimestamp);
        transaction.setAccount(newAccount);

        assertEquals(newAmount, transaction.getAmount());
        assertEquals("TRANSFER", transaction.getType());
        assertEquals(newTimestamp, transaction.getTimestamp());
        assertSame(newAccount, transaction.getAccount());
    }
}
