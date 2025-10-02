package com.example.bankapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountModelTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        account.setPassword("encodedPassword");
        account.setBalance(new BigDecimal("1000.00"));
    }

    @Test
    void testAccountCreation() {
        assertNotNull(account);
        assertEquals(1L, account.getId());
        assertEquals("testuser", account.getUsername());
        assertEquals("encodedPassword", account.getPassword());
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
    }

    @Test
    void testAccountConstructorWithParameters() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        List<Transaction> transactions = new ArrayList<>();
        
        Account newAccount = new Account(
            "newuser", 
            "password123", 
            new BigDecimal("500.00"), 
            transactions, 
            authorities
        );

        assertEquals("newuser", newAccount.getUsername());
        assertEquals("password123", newAccount.getPassword());
        assertEquals(new BigDecimal("500.00"), newAccount.getBalance());
        assertEquals(authorities, newAccount.getAuthorities());
        assertEquals(transactions, newAccount.getTransactions());
    }

    @Test
    void testSetAndGetAuthorities() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        account.setAuthorities(authorities);

        assertEquals(authorities, account.getAuthorities());
    }

    @Test
    void testBalanceUpdate() {
        account.setBalance(new BigDecimal("1500.00"));

        assertEquals(new BigDecimal("1500.00"), account.getBalance());
    }

    @Test
    void testTransactionsList() {
        Transaction transaction1 = new Transaction(new BigDecimal("100.00"), "Deposit", null, account);
        Transaction transaction2 = new Transaction(new BigDecimal("50.00"), "Withdrawal", null, account);
        List<Transaction> transactions = List.of(transaction1, transaction2);
        
        account.setTransactions(transactions);

        assertEquals(2, account.getTransactions().size());
        assertEquals(transaction1, account.getTransactions().get(0));
        assertEquals(transaction2, account.getTransactions().get(1));
    }

    @Test
    void testNullAuthorities() {
        account.setAuthorities(null);

        assertNull(account.getAuthorities());
    }
}
