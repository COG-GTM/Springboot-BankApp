package com.example.bankapp.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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
    }

    @Test
    void parameterizedConstructor_shouldSetAllFields() {
        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("USER"));
        List<Transaction> transactions = Collections.emptyList();

        Account account = new Account("john", "secret", new BigDecimal("500.00"), transactions, authorities);

        assertEquals("john", account.getUsername());
        assertEquals("secret", account.getPassword());
        assertEquals(new BigDecimal("500.00"), account.getBalance());
        assertEquals(transactions, account.getTransactions());
        assertEquals(authorities, account.getAuthorities());
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        Account account = new Account();

        account.setId(1L);
        account.setUsername("testuser");
        account.setPassword("testpass");
        account.setBalance(new BigDecimal("100.00"));

        assertEquals(1L, account.getId());
        assertEquals("testuser", account.getUsername());
        assertEquals("testpass", account.getPassword());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
    }

    @Test
    void setTransactions_shouldSetTransactions() {
        Account account = new Account();
        Transaction t1 = new Transaction();
        Transaction t2 = new Transaction();
        List<Transaction> transactions = Arrays.asList(t1, t2);

        account.setTransactions(transactions);

        assertEquals(2, account.getTransactions().size());
        assertEquals(transactions, account.getTransactions());
    }

    @Test
    void setAuthorities_shouldSetAuthorities() {
        Account account = new Account();
        Collection<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("USER"),
                new SimpleGrantedAuthority("ADMIN")
        );

        account.setAuthorities(authorities);

        assertEquals(2, account.getAuthorities().size());
    }

    @Test
    void implementsUserDetails_shouldHaveCorrectMethods() {
        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("USER"));
        Account account = new Account("user1", "pass", new BigDecimal("0"), null, authorities);

        // UserDetails interface methods
        assertEquals("user1", account.getUsername());
        assertEquals("pass", account.getPassword());
        assertNotNull(account.getAuthorities());
        // Default UserDetails methods should return true
        assertTrue(account.isAccountNonExpired());
        assertTrue(account.isAccountNonLocked());
        assertTrue(account.isCredentialsNonExpired());
        assertTrue(account.isEnabled());
    }
}
