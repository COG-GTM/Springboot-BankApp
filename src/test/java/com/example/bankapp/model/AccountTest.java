package com.example.bankapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    private String username;
    private String password;
    private BigDecimal balance;
    private List<Transaction> transactions;
    private Collection<? extends GrantedAuthority> authorities;

    @BeforeEach
    void setUp() {
        username = "alice";
        password = "secret";
        balance = new BigDecimal("100.50");
        transactions = List.of(
                new Transaction(new BigDecimal("25.00"), "DEPOSIT", LocalDateTime.now(), null));
        authorities = List.of(new SimpleGrantedAuthority("USER"));
    }

    @Test
    void allArgsConstructorSetsEveryField() {
        Account account = new Account(username, password, balance, transactions, authorities);

        assertThat(account.getUsername()).isEqualTo(username);
        assertThat(account.getPassword()).isEqualTo(password);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(account.getTransactions()).isSameAs(transactions);
        assertThat(account.getAuthorities()).isSameAs(authorities);
    }

    @Test
    void getAuthoritiesReturnsSuppliedAuthorities() {
        Account account = new Account(username, password, balance, transactions, authorities);

        assertThat(account.getAuthorities())
                .hasSize(1)
                .first()
                .extracting(GrantedAuthority::getAuthority)
                .isEqualTo("USER");
    }

    @Test
    void noArgsConstructorLeavesFieldsNull() {
        Account account = new Account();

        assertThat(account.getId()).isNull();
        assertThat(account.getUsername()).isNull();
        assertThat(account.getPassword()).isNull();
        assertThat(account.getBalance()).isNull();
        assertThat(account.getTransactions()).isNull();
        assertThat(account.getAuthorities()).isNull();
    }

    @Test
    void settersUpdateEveryField() {
        Account account = new Account();

        account.setId(42L);
        account.setUsername(username);
        account.setPassword(password);
        account.setBalance(balance);
        account.setTransactions(transactions);
        account.setAuthorities(authorities);

        assertThat(account.getId()).isEqualTo(42L);
        assertThat(account.getUsername()).isEqualTo(username);
        assertThat(account.getPassword()).isEqualTo(password);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(account.getTransactions()).isSameAs(transactions);
        assertThat(account.getAuthorities()).isSameAs(authorities);
    }

    @Test
    void getAuthoritiesReflectsSetAuthorities() {
        Account account = new Account();
        assertThat(account.getAuthorities()).isNull();

        Collection<? extends GrantedAuthority> updated =
                List.of(new SimpleGrantedAuthority("ADMIN"), new SimpleGrantedAuthority("USER"));
        account.setAuthorities(updated);

        assertThat(account.getAuthorities()).isSameAs(updated);
        assertThat(account.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ADMIN", "USER");
    }

    @Test
    void getBalanceUsesExactBigDecimalValue() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100.50"));

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("100.50"));
    }

    @Test
    void inheritedUserDetailsFlagsReturnInterfaceDefaults() {
        Account account = new Account();

        assertThat(account.isAccountNonExpired()).isTrue();
        assertThat(account.isAccountNonLocked()).isTrue();
        assertThat(account.isCredentialsNonExpired()).isTrue();
        assertThat(account.isEnabled()).isTrue();
    }
}
