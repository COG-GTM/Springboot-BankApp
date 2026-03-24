package com.example.bankapp.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void testDefaultConstructor() {
        Account account = new Account();
        assertNull(account.getId());
        assertNull(account.getUsername());
        assertNull(account.getPassword());
        assertNull(account.getBalance());
        assertNull(account.getTransactions());
        assertNull(account.getAuthorities());
    }

    @Test
    void testParameterizedConstructor() {
        List<Transaction> transactions = new ArrayList<>();
        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("USER"));

        Account account = new Account("testuser", "password123", new BigDecimal("1000.00"), transactions, authorities);

        assertEquals("testuser", account.getUsername());
        assertEquals("password123", account.getPassword());
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
        assertEquals(transactions, account.getTransactions());
        assertEquals(authorities, account.getAuthorities());
    }

    @Test
    void testGetAndSetId() {
        Account account = new Account();
        assertNull(account.getId());

        account.setId(1L);
        assertEquals(1L, account.getId());

        account.setId(null);
        assertNull(account.getId());
    }

    @Test
    void testGetAndSetUsername() {
        Account account = new Account();
        assertNull(account.getUsername());

        account.setUsername("john");
        assertEquals("john", account.getUsername());

        account.setUsername("");
        assertEquals("", account.getUsername());

        account.setUsername(null);
        assertNull(account.getUsername());
    }

    @Test
    void testGetAndSetPassword() {
        Account account = new Account();
        assertNull(account.getPassword());

        account.setPassword("secret");
        assertEquals("secret", account.getPassword());

        account.setPassword("");
        assertEquals("", account.getPassword());

        account.setPassword(null);
        assertNull(account.getPassword());
    }

    @Test
    void testGetAndSetBalance() {
        Account account = new Account();
        assertNull(account.getBalance());

        account.setBalance(new BigDecimal("500.50"));
        assertEquals(new BigDecimal("500.50"), account.getBalance());

        account.setBalance(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, account.getBalance());

        account.setBalance(null);
        assertNull(account.getBalance());
    }

    @Test
    void testGetAndSetTransactions() {
        Account account = new Account();
        assertNull(account.getTransactions());

        List<Transaction> transactions = new ArrayList<>();
        account.setTransactions(transactions);
        assertEquals(transactions, account.getTransactions());
        assertTrue(account.getTransactions().isEmpty());

        Transaction t = new Transaction();
        transactions.add(t);
        account.setTransactions(transactions);
        assertEquals(1, account.getTransactions().size());

        account.setTransactions(null);
        assertNull(account.getTransactions());
    }

    @Test
    void testGetAndSetAuthorities() {
        Account account = new Account();
        assertNull(account.getAuthorities());

        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("USER"));
        account.setAuthorities(authorities);
        assertEquals(1, account.getAuthorities().size());

        account.setAuthorities(Collections.emptyList());
        assertTrue(account.getAuthorities().isEmpty());

        account.setAuthorities(null);
        assertNull(account.getAuthorities());
    }

    @Test
    void testUserDetailsInterfaceMethods() {
        Account account = new Account("user", "pass", BigDecimal.TEN, null, null);

        // UserDetails interface methods - Account doesn't override these so they use defaults
        assertEquals("user", account.getUsername());
        assertEquals("pass", account.getPassword());
    }

    @Test
    void testParameterizedConstructorWithNullValues() {
        Account account = new Account(null, null, null, null, null);

        assertNull(account.getUsername());
        assertNull(account.getPassword());
        assertNull(account.getBalance());
        assertNull(account.getTransactions());
        assertNull(account.getAuthorities());
    }
}
