package com.example.bankapp.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void defaultConstructor_shouldCreateEmptyAccount() {
        Account account = new Account();

        assertNull(account.getId());
        assertNull(account.getUsername());
        assertNull(account.getPassword());
        assertNull(account.getBalance());
        assertNull(account.getTransactions());
        assertNull(account.getAuthorities());
    }

    @Test
    void parameterizedConstructor_shouldSetAllFields() {
        List<Transaction> transactions = Collections.emptyList();
        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("USER"));

        Account account = new Account("testuser", "password123", new BigDecimal("500"), transactions, authorities);

        assertEquals("testuser", account.getUsername());
        assertEquals("password123", account.getPassword());
        assertEquals(new BigDecimal("500"), account.getBalance());
        assertEquals(transactions, account.getTransactions());
        assertEquals(authorities, account.getAuthorities());
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        Account account = new Account();

        account.setId(1L);
        assertEquals(1L, account.getId());

        account.setUsername("user1");
        assertEquals("user1", account.getUsername());

        account.setPassword("pass1");
        assertEquals("pass1", account.getPassword());

        account.setBalance(new BigDecimal("250"));
        assertEquals(new BigDecimal("250"), account.getBalance());

        List<Transaction> transactions = Collections.emptyList();
        account.setTransactions(transactions);
        assertEquals(transactions, account.getTransactions());

        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("ADMIN"));
        account.setAuthorities(authorities);
        assertEquals(authorities, account.getAuthorities());
    }

    @Test
    void implementsUserDetails() {
        Account account = new Account();
        assertInstanceOf(UserDetails.class, account);
    }
}
