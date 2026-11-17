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
        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("USER"));
        List<Transaction> transactions = new ArrayList<>();
        
        Account account = new Account("testuser", "password123", new BigDecimal("1000.00"), transactions, authorities);
        
        assertEquals("testuser", account.getUsername());
        assertEquals("password123", account.getPassword());
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
        assertEquals(transactions, account.getTransactions());
        assertEquals(authorities, account.getAuthorities());
    }

    @Test
    void testSettersAndGetters() {
        Account account = new Account();
        
        account.setId(1L);
        account.setUsername("testuser");
        account.setPassword("password123");
        account.setBalance(new BigDecimal("500.00"));
        
        List<Transaction> transactions = new ArrayList<>();
        account.setTransactions(transactions);
        
        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("ADMIN"));
        account.setAuthorities(authorities);
        
        assertEquals(1L, account.getId());
        assertEquals("testuser", account.getUsername());
        assertEquals("password123", account.getPassword());
        assertEquals(new BigDecimal("500.00"), account.getBalance());
        assertEquals(transactions, account.getTransactions());
        assertEquals(authorities, account.getAuthorities());
    }

    @Test
    void testBalanceOperations() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100.00"));
        
        BigDecimal newBalance = account.getBalance().add(new BigDecimal("50.00"));
        account.setBalance(newBalance);
        
        assertEquals(new BigDecimal("150.00"), account.getBalance());
    }

    @Test
    void testGetAuthoritiesReturnsCorrectValue() {
        Collection<GrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority("USER"),
            new SimpleGrantedAuthority("ADMIN")
        );
        
        Account account = new Account("user", "pass", BigDecimal.ZERO, null, authorities);
        
        assertEquals(2, account.getAuthorities().size());
    }
}
