package com.example.bankapp.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    @Test
    void constructorPopulatesAllFields() {
        Transaction transaction = new Transaction(BigDecimal.ONE, "Deposit", LocalDateTime.now(), null);
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("USER"));

        Account account = new Account("alice", "secret", new BigDecimal("25.00"),
                Collections.singletonList(transaction), authorities);

        assertThat(account.getUsername()).isEqualTo("alice");
        assertThat(account.getPassword()).isEqualTo("secret");
        assertThat(account.getBalance()).isEqualByComparingTo("25.00");
        assertThat(account.getTransactions()).containsExactly(transaction);
        assertThat(account.getAuthorities()).isEqualTo(authorities);
    }

    @Test
    void settersAndGettersRoundTrip() {
        Account account = new Account();
        account.setId(7L);
        account.setUsername("bob");
        account.setPassword("hashed");
        account.setBalance(new BigDecimal("3.50"));
        account.setTransactions(Collections.emptyList());
        account.setAuthorities(Collections.singletonList(new SimpleGrantedAuthority("USER")));

        assertThat(account.getId()).isEqualTo(7L);
        assertThat(account.getUsername()).isEqualTo("bob");
        assertThat(account.getPassword()).isEqualTo("hashed");
        assertThat(account.getBalance()).isEqualByComparingTo("3.50");
        assertThat(account.getTransactions()).isEmpty();
        assertThat(account.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("USER");
    }

    @Test
    void userDetailsDefaultsReportEnabledNonExpiredAccount() {
        Account account = new Account();

        assertThat(account.isAccountNonExpired()).isTrue();
        assertThat(account.isAccountNonLocked()).isTrue();
        assertThat(account.isCredentialsNonExpired()).isTrue();
        assertThat(account.isEnabled()).isTrue();
    }
}
