package com.example.bankapp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {

    @Test
    void constructorPopulatesAllFields() {
        Account account = new Account();
        account.setUsername("alice");
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 2, 3, 4, 5);

        Transaction transaction = new Transaction(new BigDecimal("12.34"), "Deposit", timestamp, account);

        assertThat(transaction.getAmount()).isEqualByComparingTo("12.34");
        assertThat(transaction.getType()).isEqualTo("Deposit");
        assertThat(transaction.getTimestamp()).isEqualTo(timestamp);
        assertThat(transaction.getAccount()).isSameAs(account);
    }

    @Test
    void settersAndGettersRoundTrip() {
        Account account = new Account();
        LocalDateTime timestamp = LocalDateTime.of(2023, 12, 31, 23, 59);

        Transaction transaction = new Transaction();
        transaction.setId(5L);
        transaction.setAmount(new BigDecimal("99.99"));
        transaction.setType("Withdrawal");
        transaction.setTimestamp(timestamp);
        transaction.setAccount(account);

        assertThat(transaction.getId()).isEqualTo(5L);
        assertThat(transaction.getAmount()).isEqualByComparingTo("99.99");
        assertThat(transaction.getType()).isEqualTo("Withdrawal");
        assertThat(transaction.getTimestamp()).isEqualTo(timestamp);
        assertThat(transaction.getAccount()).isSameAs(account);
    }

    @Test
    void defaultConstructorLeavesFieldsUnset() {
        Transaction transaction = new Transaction();

        assertThat(transaction.getId()).isNull();
        assertThat(transaction.getAmount()).isNull();
        assertThat(transaction.getType()).isNull();
        assertThat(transaction.getTimestamp()).isNull();
        assertThat(transaction.getAccount()).isNull();
    }
}
