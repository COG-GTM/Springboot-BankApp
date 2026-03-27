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
    void noArgConstructor_createsInstance() {
        Account account = new Account();
        assertNotNull(account);
        assertNull(account.getId());
        assertNull(account.getUsername());
        assertNull(account.getPassword());
        assertNull(account.getBalance());
        assertNull(account.getTransactions());
        assertNull(account.getAuthorities());
    }

    @Test
    void parameterizedConstructor_setsAllFields() {
        Collection<? extends GrantedAuthority> authorities =
                Arrays.asList(new SimpleGrantedAuthority("USER"));
        List<Transaction> transactions = Collections.emptyList();

        Account account = new Account("testuser", "password123",
                new BigDecimal("100.00"), transactions, authorities);

        assertEquals("testuser", account.getUsername());
        assertEquals("password123", account.getPassword());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        assertEquals(transactions, account.getTransactions());
        assertEquals(authorities, account.getAuthorities());
    }

    @Test
    void settersAndGetters() {
        Account account = new Account();

        account.setId(1L);
        assertEquals(1L, account.getId());

        account.setUsername("user1");
        assertEquals("user1", account.getUsername());

        account.setPassword("pass1");
        assertEquals("pass1", account.getPassword());

        account.setBalance(new BigDecimal("250.50"));
        assertEquals(new BigDecimal("250.50"), account.getBalance());

        List<Transaction> txList = Collections.emptyList();
        account.setTransactions(txList);
        assertEquals(txList, account.getTransactions());

        Collection<? extends GrantedAuthority> auths =
                Arrays.asList(new SimpleGrantedAuthority("ADMIN"));
        account.setAuthorities(auths);
        assertEquals(auths, account.getAuthorities());
    }

    @Test
    void getAuthorities_returnsSetAuthorities() {
        Account account = new Account();
        Collection<? extends GrantedAuthority> authorities =
                Arrays.asList(new SimpleGrantedAuthority("USER"));
        account.setAuthorities(authorities);

        Collection<? extends GrantedAuthority> result = account.getAuthorities();

        assertEquals(1, result.size());
        assertEquals("USER", result.iterator().next().getAuthority());
    }
}
