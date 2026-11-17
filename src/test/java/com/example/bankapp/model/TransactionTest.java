package com.example.bankapp.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {

    // ── No-arg constructor ──────────────────────────────────────────────

    @Nested
    @DisplayName("No-arg constructor")
    class NoArgConstructor {

        @Test
        @DisplayName("creates instance with all fields null")
        void createsInstanceWithNullFields() {
            Transaction tx = new Transaction();

            assertThat(tx.getId()).isNull();
            assertThat(tx.getAmount()).isNull();
            assertThat(tx.getType()).isNull();
            assertThat(tx.getTimestamp()).isNull();
            assertThat(tx.getAccount()).isNull();
        }
    }

    // ── Parameterized constructor ───────────────────────────────────────

    @Nested
    @DisplayName("Parameterized constructor")
    class ParameterizedConstructor {

        @Test
        @DisplayName("sets all fields correctly")
        void setsAllFields() {
            Account account = new Account();
            account.setId(1L);
            BigDecimal amount = new BigDecimal("250.75");
            String type = "DEPOSIT";
            LocalDateTime timestamp = LocalDateTime.of(2025, 6, 15, 10, 30, 0);

            Transaction tx = new Transaction(amount, type, timestamp, account);

            assertThat(tx.getId()).isNull(); // id not set by this constructor
            assertThat(tx.getAmount()).isEqualByComparingTo(amount);
            assertThat(tx.getType()).isEqualTo(type);
            assertThat(tx.getTimestamp()).isEqualTo(timestamp);
            assertThat(tx.getAccount()).isSameAs(account);
        }

        @Test
        @DisplayName("accepts null values for all parameters")
        void acceptsNullValues() {
            Transaction tx = new Transaction(null, null, null, null);

            assertThat(tx.getAmount()).isNull();
            assertThat(tx.getType()).isNull();
            assertThat(tx.getTimestamp()).isNull();
            assertThat(tx.getAccount()).isNull();
        }
    }

    // ── Id getter/setter ────────────────────────────────────────────────

    @Nested
    @DisplayName("Id getter/setter")
    class IdField {

        @Test
        @DisplayName("sets and gets id")
        void setsAndGetsId() {
            Transaction tx = new Transaction();
            tx.setId(42L);

            assertThat(tx.getId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("allows null id")
        void allowsNullId() {
            Transaction tx = new Transaction();
            tx.setId(99L);
            tx.setId(null);

            assertThat(tx.getId()).isNull();
        }

        @Test
        @DisplayName("handles Long.MAX_VALUE")
        void handlesMaxValue() {
            Transaction tx = new Transaction();
            tx.setId(Long.MAX_VALUE);

            assertThat(tx.getId()).isEqualTo(Long.MAX_VALUE);
        }
    }

    // ── Amount getter/setter (BigDecimal) ───────────────────────────────

    @Nested
    @DisplayName("Amount getter/setter")
    class AmountField {

        @Test
        @DisplayName("sets and gets a positive amount")
        void positiveAmount() {
            Transaction tx = new Transaction();
            BigDecimal amount = new BigDecimal("1500.50");
            tx.setAmount(amount);

            assertThat(tx.getAmount()).isEqualByComparingTo("1500.50");
        }

        @Test
        @DisplayName("sets and gets a negative amount")
        void negativeAmount() {
            Transaction tx = new Transaction();
            tx.setAmount(new BigDecimal("-200.00"));

            assertThat(tx.getAmount()).isEqualByComparingTo("-200.00");
        }

        @Test
        @DisplayName("sets and gets zero amount")
        void zeroAmount() {
            Transaction tx = new Transaction();
            tx.setAmount(BigDecimal.ZERO);

            assertThat(tx.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("allows null amount")
        void nullAmount() {
            Transaction tx = new Transaction();
            tx.setAmount(new BigDecimal("100"));
            tx.setAmount(null);

            assertThat(tx.getAmount()).isNull();
        }

        @Test
        @DisplayName("preserves BigDecimal scale")
        void preservesScale() {
            Transaction tx = new Transaction();
            BigDecimal precise = new BigDecimal("99.9900");
            tx.setAmount(precise);

            assertThat(tx.getAmount()).isEqualTo(precise);
            assertThat(tx.getAmount().scale()).isEqualTo(4);
        }

        @Test
        @DisplayName("handles very large BigDecimal value")
        void largeValue() {
            Transaction tx = new Transaction();
            BigDecimal large = new BigDecimal("99999999999999.99");
            tx.setAmount(large);

            assertThat(tx.getAmount()).isEqualByComparingTo(large);
        }
    }

    // ── Type getter/setter ──────────────────────────────────────────────

    @Nested
    @DisplayName("Type getter/setter")
    class TypeField {

        @Test
        @DisplayName("sets and gets type DEPOSIT")
        void depositType() {
            Transaction tx = new Transaction();
            tx.setType("DEPOSIT");

            assertThat(tx.getType()).isEqualTo("DEPOSIT");
        }

        @Test
        @DisplayName("sets and gets type WITHDRAWAL")
        void withdrawalType() {
            Transaction tx = new Transaction();
            tx.setType("WITHDRAWAL");

            assertThat(tx.getType()).isEqualTo("WITHDRAWAL");
        }

        @Test
        @DisplayName("allows null type")
        void nullType() {
            Transaction tx = new Transaction();
            tx.setType("TRANSFER");
            tx.setType(null);

            assertThat(tx.getType()).isNull();
        }

        @Test
        @DisplayName("allows empty string type")
        void emptyType() {
            Transaction tx = new Transaction();
            tx.setType("");

            assertThat(tx.getType()).isEmpty();
        }

        @Test
        @DisplayName("handles unicode characters in type")
        void unicodeType() {
            Transaction tx = new Transaction();
            tx.setType("\u00DCberweisung");

            assertThat(tx.getType()).isEqualTo("\u00DCberweisung");
        }
    }

    // ── Timestamp getter/setter (LocalDateTime) ─────────────────────────

    @Nested
    @DisplayName("Timestamp getter/setter")
    class TimestampField {

        @Test
        @DisplayName("sets and gets a specific timestamp")
        void specificTimestamp() {
            Transaction tx = new Transaction();
            LocalDateTime ts = LocalDateTime.of(2025, 12, 31, 23, 59, 59);
            tx.setTimestamp(ts);

            assertThat(tx.getTimestamp()).isEqualTo(ts);
        }

        @Test
        @DisplayName("allows null timestamp")
        void nullTimestamp() {
            Transaction tx = new Transaction();
            tx.setTimestamp(LocalDateTime.now());
            tx.setTimestamp(null);

            assertThat(tx.getTimestamp()).isNull();
        }

        @Test
        @DisplayName("preserves nanosecond precision")
        void nanosecondPrecision() {
            Transaction tx = new Transaction();
            LocalDateTime ts = LocalDateTime.of(2025, 1, 1, 0, 0, 0, 123456789);
            tx.setTimestamp(ts);

            assertThat(tx.getTimestamp().getNano()).isEqualTo(123456789);
        }

        @Test
        @DisplayName("handles LocalDateTime.MIN")
        void minDateTime() {
            Transaction tx = new Transaction();
            tx.setTimestamp(LocalDateTime.MIN);

            assertThat(tx.getTimestamp()).isEqualTo(LocalDateTime.MIN);
        }

        @Test
        @DisplayName("handles LocalDateTime.MAX")
        void maxDateTime() {
            Transaction tx = new Transaction();
            tx.setTimestamp(LocalDateTime.MAX);

            assertThat(tx.getTimestamp()).isEqualTo(LocalDateTime.MAX);
        }

        @Test
        @DisplayName("can overwrite timestamp")
        void overwriteTimestamp() {
            Transaction tx = new Transaction();
            LocalDateTime first = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime second = LocalDateTime.of(2025, 6, 15, 12, 0);
            tx.setTimestamp(first);
            tx.setTimestamp(second);

            assertThat(tx.getTimestamp()).isEqualTo(second);
        }
    }

    // ── Account getter/setter (@ManyToOne) ──────────────────────────────

    @Nested
    @DisplayName("Account getter/setter (@ManyToOne relationship)")
    class AccountField {

        @Test
        @DisplayName("sets and gets account reference")
        void setsAndGetsAccount() {
            Transaction tx = new Transaction();
            Account account = new Account();
            account.setId(10L);
            account.setUsername("john_doe");
            tx.setAccount(account);

            assertThat(tx.getAccount()).isSameAs(account);
            assertThat(tx.getAccount().getId()).isEqualTo(10L);
            assertThat(tx.getAccount().getUsername()).isEqualTo("john_doe");
        }

        @Test
        @DisplayName("allows null account")
        void nullAccount() {
            Transaction tx = new Transaction();
            Account account = new Account();
            tx.setAccount(account);
            tx.setAccount(null);

            assertThat(tx.getAccount()).isNull();
        }

        @Test
        @DisplayName("can reassign to a different account")
        void reassignAccount() {
            Transaction tx = new Transaction();
            Account first = new Account();
            first.setId(1L);
            Account second = new Account();
            second.setId(2L);

            tx.setAccount(first);
            assertThat(tx.getAccount().getId()).isEqualTo(1L);

            tx.setAccount(second);
            assertThat(tx.getAccount().getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("parameterized constructor links account correctly")
        void constructorLinksAccount() {
            Account account = new Account();
            account.setId(5L);
            account.setUsername("jane");
            account.setBalance(new BigDecimal("5000.00"));

            Transaction tx = new Transaction(
                    new BigDecimal("100.00"),
                    "DEPOSIT",
                    LocalDateTime.of(2025, 3, 1, 9, 0),
                    account
            );

            assertThat(tx.getAccount()).isSameAs(account);
            assertThat(tx.getAccount().getBalance()).isEqualByComparingTo("5000.00");
        }
    }

    // ── Full round-trip / integration-style unit tests ──────────────────

    @Nested
    @DisplayName("Full round-trip")
    class RoundTrip {

        @Test
        @DisplayName("all setters then getters return consistent state")
        void fullSetterGetterRoundTrip() {
            Transaction tx = new Transaction();
            Account account = new Account();
            account.setId(7L);
            BigDecimal amount = new BigDecimal("333.33");
            LocalDateTime ts = LocalDateTime.of(2025, 7, 4, 14, 30, 0);

            tx.setId(100L);
            tx.setAmount(amount);
            tx.setType("TRANSFER");
            tx.setTimestamp(ts);
            tx.setAccount(account);

            assertThat(tx.getId()).isEqualTo(100L);
            assertThat(tx.getAmount()).isEqualByComparingTo("333.33");
            assertThat(tx.getType()).isEqualTo("TRANSFER");
            assertThat(tx.getTimestamp()).isEqualTo(ts);
            assertThat(tx.getAccount()).isSameAs(account);
        }

        @Test
        @DisplayName("two transactions can share the same account")
        void multipleTransactionsSameAccount() {
            Account account = new Account();
            account.setId(1L);

            Transaction tx1 = new Transaction(
                    new BigDecimal("50.00"), "DEPOSIT",
                    LocalDateTime.of(2025, 1, 1, 8, 0), account);
            Transaction tx2 = new Transaction(
                    new BigDecimal("25.00"), "WITHDRAWAL",
                    LocalDateTime.of(2025, 1, 2, 9, 0), account);

            assertThat(tx1.getAccount()).isSameAs(tx2.getAccount());
            assertThat(tx1.getAmount()).isNotEqualByComparingTo(tx2.getAmount());
            assertThat(tx1.getType()).isNotEqualTo(tx2.getType());
        }
    }
}
