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
        account.setId(1L);
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction(new BigDecimal("200.00"), "Deposit", now, account);

        assertEquals(new BigDecimal("200.00"), transaction.getAmount());
        assertEquals("Deposit", transaction.getType());
        assertEquals(now, transaction.getTimestamp());
        assertEquals(account, transaction.getAccount());
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        Transaction transaction = new Transaction();
        Account account = new Account();
        account.setId(2L);
        LocalDateTime now = LocalDateTime.now();

        transaction.setId(10L);
        transaction.setAmount(new BigDecimal("150.00"));
        transaction.setType("Withdrawal");
        transaction.setTimestamp(now);
        transaction.setAccount(account);

        assertEquals(10L, transaction.getId());
        assertEquals(new BigDecimal("150.00"), transaction.getAmount());
        assertEquals("Withdrawal", transaction.getType());
        assertEquals(now, transaction.getTimestamp());
        assertEquals(account, transaction.getAccount());
    }

    @Test
    void transactionTypes_shouldSupportAllTypes() {
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
