package com.example.bankapp.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    // ── No-arg constructor ──────────────────────────────────────────────

    @Test
    @DisplayName("No-arg constructor creates Account with all fields null")
    void noArgConstructor_allFieldsNull() {
        Account account = new Account();

        assertThat(account.getId()).isNull();
        assertThat(account.getUsername()).isNull();
        assertThat(account.getPassword()).isNull();
        assertThat(account.getBalance()).isNull();
        assertThat(account.getTransactions()).isNull();
        assertThat(account.getAuthorities()).isNull();
    }

    // ── Parameterized constructor ───────────────────────────────────────

    @Test
    @DisplayName("Parameterized constructor sets all supplied fields")
    void parameterizedConstructor_setsAllFields() {
        List<Transaction> transactions = List.of(new Transaction());
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        Account account = new Account("alice", "secret", new BigDecimal("1000.50"), transactions, authorities);

        assertThat(account.getUsername()).isEqualTo("alice");
        assertThat(account.getPassword()).isEqualTo("secret");
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("1000.50"));
        assertThat(account.getTransactions()).isSameAs(transactions);
        assertThat(account.getAuthorities()).isSameAs(authorities);
        // id is not set by the parameterized constructor
        assertThat(account.getId()).isNull();
    }

    @Test
    @DisplayName("Parameterized constructor accepts null values")
    void parameterizedConstructor_nullValues() {
        Account account = new Account(null, null, null, null, null);

        assertThat(account.getUsername()).isNull();
        assertThat(account.getPassword()).isNull();
        assertThat(account.getBalance()).isNull();
        assertThat(account.getTransactions()).isNull();
        assertThat(account.getAuthorities()).isNull();
    }

    // ── id getter/setter ────────────────────────────────────────────────

    @Test
    @DisplayName("setId/getId round-trips a value")
    void setAndGetId() {
        Account account = new Account();
        account.setId(42L);

        assertThat(account.getId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("setId accepts null")
    void setId_null() {
        Account account = new Account();
        account.setId(99L);
        account.setId(null);

        assertThat(account.getId()).isNull();
    }

    // ── username getter/setter ──────────────────────────────────────────

    @Test
    @DisplayName("setUsername/getUsername round-trips a value")
    void setAndGetUsername() {
        Account account = new Account();
        account.setUsername("bob");

        assertThat(account.getUsername()).isEqualTo("bob");
    }

    @Test
    @DisplayName("setUsername accepts empty string")
    void setUsername_empty() {
        Account account = new Account();
        account.setUsername("");

        assertThat(account.getUsername()).isEmpty();
    }

    @Test
    @DisplayName("setUsername accepts null")
    void setUsername_null() {
        Account account = new Account();
        account.setUsername("temp");
        account.setUsername(null);

        assertThat(account.getUsername()).isNull();
    }

    @Test
    @DisplayName("setUsername handles unicode characters")
    void setUsername_unicode() {
        Account account = new Account();
        account.setUsername("用户名");

        assertThat(account.getUsername()).isEqualTo("用户名");
    }

    // ── password getter/setter ──────────────────────────────────────────

    @Test
    @DisplayName("setPassword/getPassword round-trips a value")
    void setAndGetPassword() {
        Account account = new Account();
        account.setPassword("p@ssw0rd!");

        assertThat(account.getPassword()).isEqualTo("p@ssw0rd!");
    }

    @Test
    @DisplayName("setPassword accepts null")
    void setPassword_null() {
        Account account = new Account();
        account.setPassword(null);

        assertThat(account.getPassword()).isNull();
    }

    // ── balance getter/setter (BigDecimal) ──────────────────────────────

    @Test
    @DisplayName("setBalance/getBalance round-trips a BigDecimal")
    void setAndGetBalance() {
        Account account = new Account();
        account.setBalance(new BigDecimal("12345.67"));

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("12345.67"));
    }

    @Test
    @DisplayName("balance preserves BigDecimal scale")
    void balance_preservesScale() {
        Account account = new Account();
        BigDecimal precise = new BigDecimal("100.00");
        account.setBalance(precise);

        assertThat(account.getBalance()).isEqualTo(precise);
        assertThat(account.getBalance().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("balance handles zero")
    void balance_zero() {
        Account account = new Account();
        account.setBalance(BigDecimal.ZERO);

        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("balance handles negative values")
    void balance_negative() {
        Account account = new Account();
        account.setBalance(new BigDecimal("-500.25"));

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("-500.25"));
    }

    @Test
    @DisplayName("balance handles very large values")
    void balance_veryLarge() {
        Account account = new Account();
        BigDecimal large = new BigDecimal("99999999999999999.99");
        account.setBalance(large);

        assertThat(account.getBalance()).isEqualByComparingTo(large);
    }

    @Test
    @DisplayName("setBalance accepts null")
    void balance_null() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100"));
        account.setBalance(null);

        assertThat(account.getBalance()).isNull();
    }

    // ── transactions getter/setter ──────────────────────────────────────

    @Test
    @DisplayName("setTransactions/getTransactions round-trips a list")
    void setAndGetTransactions() {
        Account account = new Account();
        List<Transaction> txns = new ArrayList<>();
        txns.add(new Transaction());
        account.setTransactions(txns);

        assertThat(account.getTransactions()).hasSize(1);
        assertThat(account.getTransactions()).isSameAs(txns);
    }

    @Test
    @DisplayName("setTransactions accepts empty list")
    void setTransactions_empty() {
        Account account = new Account();
        account.setTransactions(Collections.emptyList());

        assertThat(account.getTransactions()).isEmpty();
    }

    @Test
    @DisplayName("setTransactions accepts null")
    void setTransactions_null() {
        Account account = new Account();
        account.setTransactions(null);

        assertThat(account.getTransactions()).isNull();
    }

    // ── authorities getter/setter (@Transient) ──────────────────────────

    @Test
    @DisplayName("setAuthorities/getAuthorities round-trips a collection")
    void setAndGetAuthorities() {
        Account account = new Account();
        Collection<GrantedAuthority> auths = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN"));
        account.setAuthorities(auths);

        assertThat(account.getAuthorities())
                .hasSize(2)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("setAuthorities accepts empty collection")
    void setAuthorities_empty() {
        Account account = new Account();
        account.setAuthorities(Collections.emptyList());

        assertThat(account.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("setAuthorities accepts null")
    void setAuthorities_null() {
        Account account = new Account();
        account.setAuthorities(null);

        assertThat(account.getAuthorities()).isNull();
    }

    @Test
    @DisplayName("authorities field is @Transient and not persisted")
    void authorities_isTransient() throws NoSuchFieldException {
        var field = Account.class.getDeclaredField("authorities");
        assertThat(field.isAnnotationPresent(jakarta.persistence.Transient.class)).isTrue();
    }

    // ── UserDetails interface methods ───────────────────────────────────

    @Test
    @DisplayName("getUsername returns username set via constructor")
    void userDetails_getUsername() {
        Account account = new Account("charlie", "pass", BigDecimal.TEN, null, null);

        assertThat(account.getUsername()).isEqualTo("charlie");
    }

    @Test
    @DisplayName("getPassword returns password set via constructor")
    void userDetails_getPassword() {
        Account account = new Account("charlie", "pass", BigDecimal.TEN, null, null);

        assertThat(account.getPassword()).isEqualTo("pass");
    }

    @Test
    @DisplayName("getAuthorities returns authorities set via constructor")
    void userDetails_getAuthorities() {
        Collection<GrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        Account account = new Account("charlie", "pass", BigDecimal.TEN, null, auths);

        assertThat(account.getAuthorities()).hasSize(1);
        assertThat(account.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("isAccountNonExpired returns true (default UserDetails)")
    void userDetails_isAccountNonExpired() {
        Account account = new Account();

        assertThat(account.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("isAccountNonLocked returns true (default UserDetails)")
    void userDetails_isAccountNonLocked() {
        Account account = new Account();

        assertThat(account.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("isCredentialsNonExpired returns true (default UserDetails)")
    void userDetails_isCredentialsNonExpired() {
        Account account = new Account();

        assertThat(account.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("isEnabled returns true (default UserDetails)")
    void userDetails_isEnabled() {
        Account account = new Account();

        assertThat(account.isEnabled()).isTrue();
    }

    // ── JPA annotation verification ─────────────────────────────────────

    @Test
    @DisplayName("Account class is annotated with @Entity")
    void classHasEntityAnnotation() {
        assertThat(Account.class.isAnnotationPresent(jakarta.persistence.Entity.class)).isTrue();
    }

    @Test
    @DisplayName("id field has @Id and @GeneratedValue annotations")
    void idField_hasJpaAnnotations() throws NoSuchFieldException {
        var field = Account.class.getDeclaredField("id");
        assertThat(field.isAnnotationPresent(jakarta.persistence.Id.class)).isTrue();
        assertThat(field.isAnnotationPresent(jakarta.persistence.GeneratedValue.class)).isTrue();

        var genVal = field.getAnnotation(jakarta.persistence.GeneratedValue.class);
        assertThat(genVal.strategy()).isEqualTo(jakarta.persistence.GenerationType.IDENTITY);
    }

    @Test
    @DisplayName("transactions field has @OneToMany with mappedBy='account'")
    void transactionsField_hasOneToMany() throws NoSuchFieldException {
        var field = Account.class.getDeclaredField("transactions");
        assertThat(field.isAnnotationPresent(jakarta.persistence.OneToMany.class)).isTrue();

        var oneToMany = field.getAnnotation(jakarta.persistence.OneToMany.class);
        assertThat(oneToMany.mappedBy()).isEqualTo("account");
    }

    // ── Setter overwrite behavior ───────────────────────────────────────

    @Test
    @DisplayName("Setters overwrite previously set values")
    void settersOverwriteValues() {
        Account account = new Account("original", "oldPass", new BigDecimal("100"), null, null);

        account.setUsername("updated");
        account.setPassword("newPass");
        account.setBalance(new BigDecimal("999.99"));

        assertThat(account.getUsername()).isEqualTo("updated");
        assertThat(account.getPassword()).isEqualTo("newPass");
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("999.99"));
    }

    // ── Account implements UserDetails ───────────────────────────────────

    @Test
    @DisplayName("Account implements UserDetails interface")
    void accountImplementsUserDetails() {
        Account account = new Account();

        assertThat(account).isInstanceOf(org.springframework.security.core.userdetails.UserDetails.class);
    }
}
