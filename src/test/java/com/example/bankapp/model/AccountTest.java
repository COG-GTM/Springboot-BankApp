package com.example.bankapp.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void testNoArgConstructor() {
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
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("USER"));

        Account account = new Account("testuser", "password123", BigDecimal.TEN, transactions, authorities);

        assertEquals("testuser", account.getUsername());
        assertEquals("password123", account.getPassword());
        assertEquals(BigDecimal.TEN, account.getBalance());
        assertEquals(transactions, account.getTransactions());
        assertEquals(authorities, account.getAuthorities());
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
        account.setUsername("user1");
        assertEquals("user1", account.getUsername());
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
        account.setBalance(new BigDecimal("100.50"));
        assertEquals(new BigDecimal("100.50"), account.getBalance());
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
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ADMIN"));
        account.setAuthorities(authorities);
        assertEquals(1, account.getAuthorities().size());
    }

    @Test
    void testNullValues() {
        Account account = new Account(null, null, null, null, null);
        assertNull(account.getUsername());
        assertNull(account.getPassword());
        assertNull(account.getBalance());
        assertNull(account.getTransactions());
        assertNull(account.getAuthorities());
    }

    @Test
    void testEmptyStringValues() {
        Account account = new Account();
        account.setUsername("");
        account.setPassword("");
        assertEquals("", account.getUsername());
        assertEquals("", account.getPassword());
    }
}
