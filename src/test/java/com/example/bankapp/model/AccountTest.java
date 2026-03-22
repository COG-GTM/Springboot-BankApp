package com.example.bankapp.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void testNoArgConstructor() {
        Account account = new Account();
        assertNotNull(account);
    }

    @Test
    void testParameterizedConstructor() {
        List<Transaction> transactions = new ArrayList<>();
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("USER"));

        Account account = new Account("testuser", "password123", new BigDecimal("1000.00"), transactions, authorities);

        assertEquals("testuser", account.getUsername());
        assertEquals("password123", account.getPassword());
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
        assertEquals(transactions, account.getTransactions());
        assertEquals(authorities, account.getAuthorities());
    }

    @Test
    void testGetSetId() {
        Account account = new Account();
        account.setId(1L);
        assertEquals(1L, account.getId());
    }

    @Test
    void testGetSetUsername() {
        Account account = new Account();
        account.setUsername("john");
        assertEquals("john", account.getUsername());
    }

    @Test
    void testGetSetPassword() {
        Account account = new Account();
        account.setPassword("secret");
        assertEquals("secret", account.getPassword());
    }

    @Test
    void testGetSetBalance() {
        Account account = new Account();
        account.setBalance(new BigDecimal("500.50"));
        assertEquals(new BigDecimal("500.50"), account.getBalance());
    }

    @Test
    void testGetSetTransactions() {
        Account account = new Account();
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction());
        account.setTransactions(transactions);
        assertEquals(1, account.getTransactions().size());
    }

    @Test
    void testGetSetAuthorities() {
        Account account = new Account();
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ADMIN"));
        account.setAuthorities(authorities);
        assertEquals(1, account.getAuthorities().size());
    }

    @Test
    void testIsAccountNonExpired() {
        Account account = new Account();
        assertTrue(account.isAccountNonExpired());
    }

    @Test
    void testIsAccountNonLocked() {
        Account account = new Account();
        assertTrue(account.isAccountNonLocked());
    }

    @Test
    void testIsCredentialsNonExpired() {
        Account account = new Account();
        assertTrue(account.isCredentialsNonExpired());
    }

    @Test
    void testIsEnabled() {
        Account account = new Account();
        assertTrue(account.isEnabled());
    }
}
