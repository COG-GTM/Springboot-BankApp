package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CHARACTERIZATION TESTS for {@link AccountService}.
 *
 * <p>These tests pin down the CURRENT, as-shipped behavior of registration,
 * deposit, withdraw, and transfer -- including edge cases that today are NOT
 * validated (non-positive amounts, self-transfer). They form the safety net
 * that lets us remediate the service with confidence.
 *
 * <p>Tests whose names contain {@code current_} document behavior that is
 * presently INCORRECT/UNSAFE. The remediation phase updates these to assert the
 * new, validated behavior.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceCharacterizationTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    private Account accountWithBalance(String username, String balance) {
        Account account = new Account();
        account.setUsername(username);
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    @Nested
    @DisplayName("registerAccount")
    class RegisterAccount {

        @Test
        @DisplayName("creates a new account with an encoded password and a zero opening balance")
        void register_newUsername_encodesPasswordAndZeroBalance() {
            when(accountRepository.findByUsername("alice")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret")).thenReturn("ENCODED");
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            Account result = accountService.registerAccount("alice", "secret");

            assertThat(result.getUsername()).isEqualTo("alice");
            assertThat(result.getPassword()).isEqualTo("ENCODED");
            assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("rejects a duplicate username")
        void register_duplicateUsername_throws() {
            when(accountRepository.findByUsername("bob"))
                    .thenReturn(Optional.of(accountWithBalance("bob", "0")));

            assertThatThrownBy(() -> accountService.registerAccount("bob", "pw"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Username already exists");
            verify(accountRepository, never()).save(any(Account.class));
        }
    }

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        @DisplayName("adds a positive amount to the balance and records a Deposit transaction")
        void deposit_positiveAmount_increasesBalanceAndRecordsTransaction() {
            Account account = accountWithBalance("alice", "100.00");

            accountService.deposit(account, new BigDecimal("50.00"));

            assertThat(account.getBalance()).isEqualByComparingTo("150.00");
            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo("Deposit");
            assertThat(captor.getValue().getAmount()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("CURRENT (unsafe): a negative deposit is accepted and DECREASES the balance")
        void deposit_negativeAmount_currentlyDecreasesBalance() {
            Account account = accountWithBalance("alice", "100.00");

            accountService.deposit(account, new BigDecimal("-50.00"));

            assertThat(account.getBalance()).isEqualByComparingTo("50.00");
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("CURRENT (unsafe): a zero deposit is accepted and records a transaction")
        void deposit_zeroAmount_currentlyAccepted() {
            Account account = accountWithBalance("alice", "100.00");

            accountService.deposit(account, BigDecimal.ZERO);

            assertThat(account.getBalance()).isEqualByComparingTo("100.00");
            verify(transactionRepository).save(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("subtracts a valid amount and records a Withdrawal transaction")
        void withdraw_sufficientFunds_decreasesBalanceAndRecordsTransaction() {
            Account account = accountWithBalance("alice", "100.00");

            accountService.withdraw(account, new BigDecimal("40.00"));

            assertThat(account.getBalance()).isEqualByComparingTo("60.00");
            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo("Withdrawal");
        }

        @Test
        @DisplayName("rejects a withdrawal that exceeds the balance")
        void withdraw_insufficientFunds_throws() {
            Account account = accountWithBalance("alice", "30.00");

            assertThatThrownBy(() -> accountService.withdraw(account, new BigDecimal("40.00")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Insufficient funds");
            verify(accountRepository, never()).save(any(Account.class));
            verify(transactionRepository, never()).save(any(Transaction.class));
        }

        @Test
        @DisplayName("CURRENT (unsafe): a negative withdrawal passes the funds check and INCREASES the balance")
        void withdraw_negativeAmount_currentlyIncreasesBalance() {
            Account account = accountWithBalance("alice", "100.00");

            accountService.withdraw(account, new BigDecimal("-50.00"));

            assertThat(account.getBalance()).isEqualByComparingTo("150.00");
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("CURRENT (unsafe): a zero withdrawal is accepted and records a transaction")
        void withdraw_zeroAmount_currentlyAccepted() {
            Account account = accountWithBalance("alice", "100.00");

            accountService.withdraw(account, BigDecimal.ZERO);

            assertThat(account.getBalance()).isEqualByComparingTo("100.00");
            verify(transactionRepository).save(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("transferAmount")
    class Transfer {

        @Test
        @DisplayName("moves funds between two accounts and records debit and credit transactions")
        void transfer_valid_movesFundsAndRecordsBothLegs() {
            Account from = accountWithBalance("alice", "100.00");
            Account to = accountWithBalance("bob", "20.00");
            when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(to));

            accountService.transferAmount(from, "bob", new BigDecimal("30.00"));

            assertThat(from.getBalance()).isEqualByComparingTo("70.00");
            assertThat(to.getBalance()).isEqualByComparingTo("50.00");
            verify(transactionRepository, times(2)).save(any(Transaction.class));
        }

        @Test
        @DisplayName("rejects a transfer that exceeds the sender balance (checked before recipient lookup)")
        void transfer_insufficientFunds_throwsBeforeRecipientLookup() {
            Account from = accountWithBalance("alice", "10.00");

            assertThatThrownBy(() -> accountService.transferAmount(from, "bob", new BigDecimal("30.00")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Insufficient funds");
            verify(accountRepository, never()).findByUsername("bob");
            verify(transactionRepository, never()).save(any(Transaction.class));
        }

        @Test
        @DisplayName("rejects a transfer to a recipient that does not exist")
        void transfer_recipientNotFound_throws() {
            Account from = accountWithBalance("alice", "100.00");
            when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.transferAmount(from, "ghost", new BigDecimal("30.00")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Recipient account not found");
            verify(transactionRepository, never()).save(any(Transaction.class));
        }

        @Test
        @DisplayName("CURRENT (unsafe): a negative transfer STEALS from the recipient and credits the sender")
        void transfer_negativeAmount_currentlyStealsFromRecipient() {
            Account from = accountWithBalance("alice", "100.00");
            Account to = accountWithBalance("bob", "100.00");
            when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(to));

            accountService.transferAmount(from, "bob", new BigDecimal("-40.00"));

            assertThat(from.getBalance()).isEqualByComparingTo("140.00");
            assertThat(to.getBalance()).isEqualByComparingTo("60.00");
            verify(transactionRepository, times(2)).save(any(Transaction.class));
        }

        @Test
        @DisplayName("CURRENT (unsafe): a self-transfer is accepted and records spurious transactions")
        void transfer_toSelf_currentlyAccepted() {
            Account self = accountWithBalance("alice", "100.00");
            when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(self));

            accountService.transferAmount(self, "alice", new BigDecimal("30.00"));

            assertThat(self.getBalance()).isEqualByComparingTo("100.00");
            verify(transactionRepository, times(2)).save(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("getTransactionHistory")
    class History {

        @Test
        @DisplayName("returns the transactions recorded for the account")
        void history_returnsRepositoryResults() {
            Account account = accountWithBalance("alice", "100.00");
            account.setId(7L);
            List<Transaction> expected = Collections.singletonList(
                    new Transaction(new BigDecimal("5.00"), "Deposit", null, account));
            when(transactionRepository.findByAccountId(7L)).thenReturn(expected);

            assertThat(accountService.getTransactionHistory(account)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUser {

        @Test
        @DisplayName("loads a user with the single hardcoded USER authority")
        void loadUser_returnsUserDetailsWithUserAuthority() {
            Account stored = accountWithBalance("alice", "100.00");
            stored.setPassword("ENCODED");
            when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(stored));

            UserDetails details = accountService.loadUserByUsername("alice");

            assertThat(details.getUsername()).isEqualTo("alice");
            assertThat(details.getAuthorities())
                    .extracting("authority")
                    .containsExactly("USER");
        }
    }
}
