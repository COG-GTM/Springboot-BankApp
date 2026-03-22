package com.example.bankapp.model;

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

    @Test
    void testNoArgsConstructor() {
        Account account = new Account();
        assertNull(account.getId());
        assertNull(account.getUsername());
        assertNull(account.getPassword());
        assertNull(account.getBalance());
        assertNull(account.getTransactions());
        assertNull(account.getAuthorities());
    }

    @Test
    void testAllArgsConstructor() {
        List<Transaction> transactions = new ArrayList<>();
        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("USER"));

        Account account = new Account("testuser", "password123", new BigDecimal("1000.00"), transactions, authorities);

        assertEquals("testuser", account.getUsername());
        assertEquals("password123", account.getPassword());
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
        assertSame(transactions, account.getTransactions());
        assertSame(authorities, account.getAuthorities());
    }

    @Test
    void testSetAndGetId() {
        Account account = new Account();
        account.setId(1L);
        assertEquals(1L, account.getId());
    }

    @Test
    void testSetAndGetUsername() {
        Account account = new Account();
        account.setUsername("john");
        assertEquals("john", account.getUsername());
    }

    @Test
    void testSetAndGetPassword() {
        Account account = new Account();
        account.setPassword("secret");
        assertEquals("secret", account.getPassword());
    }

    @Test
    void testSetAndGetBalance() {
        Account account = new Account();
        account.setBalance(new BigDecimal("500.50"));
        assertEquals(new BigDecimal("500.50"), account.getBalance());
    }

    @Test
    void testSetAndGetTransactions() {
        Account account = new Account();
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction());
        account.setTransactions(transactions);
        assertEquals(1, account.getTransactions().size());
    }

    @Test
    void testSetAndGetAuthorities() {
        Account account = new Account();
        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("ADMIN"));
        account.setAuthorities(authorities);
        assertEquals(1, account.getAuthorities().size());
    }

    @Test
    void testUserDetailsInterfaceMethods() {
        Account account = new Account("user", "pass", BigDecimal.ZERO, null, null);
        // UserDetails interface methods - Account doesn't override these so they return defaults
        assertEquals("user", account.getUsername());
        assertEquals("pass", account.getPassword());
    }

    @Test
    void testSetIdToNull() {
        Account account = new Account();
        account.setId(5L);
        assertEquals(5L, account.getId());
        account.setId(null);
        assertNull(account.getId());
    }

    @Test
    void testSetBalanceToZero() {
        Account account = new Account();
        account.setBalance(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    void testSetTransactionsToNull() {
        Account account = new Account();
        account.setTransactions(null);
        assertNull(account.getTransactions());
    }

    @Test
    void testSetAuthoritiesToNull() {
        Account account = new Account();
        account.setAuthorities(null);
        assertNull(account.getAuthorities());
    }

    @Test
    void testConstructorWithNullValues() {
        Account account = new Account(null, null, null, null, null);
        assertNull(account.getUsername());
        assertNull(account.getPassword());
        assertNull(account.getBalance());
        assertNull(account.getTransactions());
        assertNull(account.getAuthorities());
    }
}
