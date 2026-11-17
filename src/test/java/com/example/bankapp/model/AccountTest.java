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
    void testSetAndGetId() {
        Account account = new Account();
        account.setId(1L);
        assertEquals(1L, account.getId());
    }

    @Test
    void testSetAndGetUsername() {
        Account account = new Account();
        account.setUsername("testuser");
        assertEquals("testuser", account.getUsername());
    }

    @Test
    void testSetAndGetPassword() {
        Account account = new Account();
        account.setPassword("password123");
        assertEquals("password123", account.getPassword());
    }

    @Test
    void testSetAndGetBalance() {
        Account account = new Account();
        BigDecimal balance = new BigDecimal("500.50");
        account.setBalance(balance);
        assertEquals(balance, account.getBalance());
    }

    @Test
    void testSetAndGetTransactions() {
        Account account = new Account();
        List<Transaction> transactions = new ArrayList<>();
        Transaction transaction = new Transaction();
        transactions.add(transaction);
        account.setTransactions(transactions);
        assertEquals(transactions, account.getTransactions());
        assertEquals(1, account.getTransactions().size());
    }

    @Test
    void testSetAndGetAuthorities() {
        Account account = new Account();
        Collection<GrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority("USER"),
            new SimpleGrantedAuthority("ADMIN")
        );
        account.setAuthorities(authorities);
        assertEquals(authorities, account.getAuthorities());
        assertEquals(2, account.getAuthorities().size());
    }

    @Test
    void testUserDetailsImplementation() {
        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("USER"));
        Account account = new Account("testuser", "password123", new BigDecimal("1000.00"), null, authorities);
        
        assertEquals("testuser", account.getUsername());
        assertEquals("password123", account.getPassword());
        assertEquals(authorities, account.getAuthorities());
    }
}
