package com.example.bankapp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testNoArgConstructor() {
        Transaction transaction = new Transaction();
        assertNotNull(transaction);
    }

    @Test
    void testParameterizedConstructor() {
        Account account = new Account();
        account.setUsername("testuser");
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction(new BigDecimal("100.00"), "Deposit", now, account);

        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals("Deposit", transaction.getType());
        assertEquals(now, transaction.getTimestamp());
        assertEquals(account, transaction.getAccount());
    }

    @Test
    void testGetSetId() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        assertEquals(1L, transaction.getId());
    }

    @Test
    void testGetSetAmount() {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("250.75"));
        assertEquals(new BigDecimal("250.75"), transaction.getAmount());
    }

    @Test
    void testGetSetType() {
        Transaction transaction = new Transaction();
        transaction.setType("Withdrawal");
        assertEquals("Withdrawal", transaction.getType());
    }

    @Test
    void testGetSetTimestamp() {
        Transaction transaction = new Transaction();
        LocalDateTime now = LocalDateTime.now();
        transaction.setTimestamp(now);
        assertEquals(now, transaction.getTimestamp());
    }

    @Test
    void testGetSetAccount() {
        Transaction transaction = new Transaction();
        Account account = new Account();
        account.setUsername("user1");
        transaction.setAccount(account);
        assertEquals("user1", transaction.getAccount().getUsername());
    }

    @Test
    void testSetAmountToZero() {
        Transaction transaction = new Transaction();
        transaction.setAmount(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, transaction.getAmount());
    }

    @Test
    void testSetTypeDeposit() {
        Transaction transaction = new Transaction();
        transaction.setType("Deposit");
        assertEquals("Deposit", transaction.getType());
    }

    @Test
    void testSetTypeTransfer() {
        Transaction transaction = new Transaction();
        transaction.setType("Transfer Out to user2");
        assertEquals("Transfer Out to user2", transaction.getType());
    }
}
