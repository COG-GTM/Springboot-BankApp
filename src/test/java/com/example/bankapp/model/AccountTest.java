package com.example.bankapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
    }

    @Test
    void defaultConstructor_CreatesEmptyAccount() {
        Account newAccount = new Account();
        assertNotNull(newAccount);
        assertNull(newAccount.getId());
        assertNull(newAccount.getUsername());
        assertNull(newAccount.getPassword());
        assertNull(newAccount.getBalance());
    }

    @Test
    void parameterizedConstructor_SetsAllFields() {
        List<Transaction> transactions = new ArrayList<>();
        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("USER"));

        Account newAccount = new Account("testuser", "password123", new BigDecimal("1000.00"), transactions, authorities);

        assertEquals("testuser", newAccount.getUsername());
        assertEquals("password123", newAccount.getPassword());
        assertEquals(new BigDecimal("1000.00"), newAccount.getBalance());
        assertEquals(transactions, newAccount.getTransactions());
        assertEquals(authorities, newAccount.getAuthorities());
    }

    @Test
    void setAndGetId_WorksCorrectly() {
        account.setId(1L);
        assertEquals(1L, account.getId());
    }

    @Test
    void setAndGetUsername_WorksCorrectly() {
        account.setUsername("testuser");
        assertEquals("testuser", account.getUsername());
    }

    @Test
    void setAndGetPassword_WorksCorrectly() {
        account.setPassword("password123");
        assertEquals("password123", account.getPassword());
    }

    @Test
    void setAndGetBalance_WorksCorrectly() {
        BigDecimal balance = new BigDecimal("500.00");
        account.setBalance(balance);
        assertEquals(balance, account.getBalance());
    }

    @Test
    void setAndGetTransactions_WorksCorrectly() {
        List<Transaction> transactions = new ArrayList<>();
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transactions.add(transaction);

        account.setTransactions(transactions);
        assertEquals(transactions, account.getTransactions());
        assertEquals(1, account.getTransactions().size());
    }

    @Test
    void setAndGetAuthorities_WorksCorrectly() {
        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("USER"));
        account.setAuthorities(authorities);
        assertEquals(authorities, account.getAuthorities());
    }

    @Test
    void getAuthorities_WhenNotSet_ReturnsNull() {
        assertNull(account.getAuthorities());
    }

    @Test
    void balance_CanBeZero() {
        account.setBalance(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    void balance_CanBeNegative() {
        BigDecimal negativeBalance = new BigDecimal("-100.00");
        account.setBalance(negativeBalance);
        assertEquals(negativeBalance, account.getBalance());
    }

    @Test
    void transactions_CanBeEmptyList() {
        List<Transaction> emptyTransactions = new ArrayList<>();
        account.setTransactions(emptyTransactions);
        assertNotNull(account.getTransactions());
        assertTrue(account.getTransactions().isEmpty());
    }
}
