package com.example.bankapp.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void noArgConstructor_createsInstance() {
        Account account = new Account();
        assertNotNull(account);
    }

    @Test
    void parameterizedConstructor_setsAllFields() {
        List<Transaction> transactions = Collections.emptyList();
        var authorities = List.of(new SimpleGrantedAuthority("USER"));

        Account account = new Account("testuser", "password", new BigDecimal("100.00"), transactions, authorities);

        assertEquals("testuser", account.getUsername());
        assertEquals("password", account.getPassword());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        assertEquals(transactions, account.getTransactions());
        assertEquals(authorities, account.getAuthorities());
    }

    @Test
    void gettersAndSetters_id() {
        Account account = new Account();
        account.setId(1L);
        assertEquals(1L, account.getId());
    }

    @Test
    void gettersAndSetters_username() {
        Account account = new Account();
        account.setUsername("user1");
        assertEquals("user1", account.getUsername());
    }

    @Test
    void gettersAndSetters_password() {
        Account account = new Account();
        account.setPassword("secret");
        assertEquals("secret", account.getPassword());
    }

    @Test
    void gettersAndSetters_balance() {
        Account account = new Account();
        account.setBalance(new BigDecimal("250.50"));
        assertEquals(new BigDecimal("250.50"), account.getBalance());
    }

    @Test
    void gettersAndSetters_transactions() {
        Account account = new Account();
        List<Transaction> transactions = List.of(new Transaction());
        account.setTransactions(transactions);
        assertEquals(transactions, account.getTransactions());
    }

    @Test
    void gettersAndSetters_authorities() {
        Account account = new Account();
        var authorities = List.of(new SimpleGrantedAuthority("ADMIN"));
        account.setAuthorities(authorities);
        assertEquals(authorities, account.getAuthorities());
    }

    @Test
    void isAccountNonExpired_returnsTrue() {
        Account account = new Account();
        assertTrue(account.isAccountNonExpired());
    }

    @Test
    void isAccountNonLocked_returnsTrue() {
        Account account = new Account();
        assertTrue(account.isAccountNonLocked());
    }

    @Test
    void isCredentialsNonExpired_returnsTrue() {
        Account account = new Account();
        assertTrue(account.isCredentialsNonExpired());
    }

    @Test
    void isEnabled_returnsTrue() {
        Account account = new Account();
        assertTrue(account.isEnabled());
    }
}
