package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUsername("john");
        testAccount.setPassword("encodedPassword");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setTransactions(Collections.emptyList());
    }

    // ─── findAccountByUsername ───────────────────────────────────────────

    @Nested
    @DisplayName("findAccountByUsername")
    class FindAccountByUsername {

        @Test
        @DisplayName("returns account when username exists")
        void returnsAccountWhenFound() {
            when(accountRepository.findByUsername("john")).thenReturn(Optional.of(testAccount));

            Account result = accountService.findAccountByUsername("john");

            assertThat(result).isSameAs(testAccount);
            assertThat(result.getUsername()).isEqualTo("john");
            verify(accountRepository).findByUsername("john");
        }

        @Test
        @DisplayName("throws RuntimeException when username not found")
        void throwsWhenNotFound() {
            when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.findAccountByUsername("unknown"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Account not found");
        }
    }

    // ─── registerAccount ────────────────────────────────────────────────

    @Nested
    @DisplayName("registerAccount")
    class RegisterAccount {

        @Test
        @DisplayName("registers new account with encoded password and zero balance")
        void registersNewAccount() {
            when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("rawPass")).thenReturn("encodedPass");
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            Account result = accountService.registerAccount("newuser", "rawPass");

            assertThat(result.getUsername()).isEqualTo("newuser");
            assertThat(result.getPassword()).isEqualTo("encodedPass");
            assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

            verify(passwordEncoder).encode("rawPass");
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("throws when username already exists")
        void throwsOnDuplicateUsername() {
            when(accountRepository.findByUsername("john")).thenReturn(Optional.of(testAccount));

            assertThatThrownBy(() -> accountService.registerAccount("john", "pass"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Username already exists");

            verify(accountRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // ─── deposit ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        @DisplayName("increases balance and creates deposit transaction")
        void depositsSuccessfully() {
            BigDecimal depositAmount = new BigDecimal("500.00");

            accountService.deposit(testAccount, depositAmount);

            assertThat(testAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
            verify(accountRepository).save(testAccount);

            ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(txCaptor.capture());
            Transaction savedTx = txCaptor.getValue();
            assertThat(savedTx.getAmount()).isEqualByComparingTo(depositAmount);
            assertThat(savedTx.getType()).isEqualTo("Deposit");
            assertThat(savedTx.getAccount()).isSameAs(testAccount);
            assertThat(savedTx.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("deposit of zero still saves account and transaction")
        void depositZero() {
            accountService.deposit(testAccount, BigDecimal.ZERO);

            assertThat(testAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
            verify(accountRepository).save(testAccount);
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("deposit of a small fractional amount")
        void depositFractionalAmount() {
            BigDecimal amount = new BigDecimal("0.01");

            accountService.deposit(testAccount, amount);

            assertThat(testAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1000.01"));
            verify(accountRepository).save(testAccount);
        }

        @Test
        @DisplayName("deposit of a large amount")
        void depositLargeAmount() {
            BigDecimal amount = new BigDecimal("999999999.99");

            accountService.deposit(testAccount, amount);

            assertThat(testAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1000000999.99"));
            verify(accountRepository).save(testAccount);
        }
    }

    // ─── withdraw ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("decreases balance and creates withdrawal transaction")
        void withdrawsSuccessfully() {
            BigDecimal withdrawAmount = new BigDecimal("300.00");

            accountService.withdraw(testAccount, withdrawAmount);

            assertThat(testAccount.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
            verify(accountRepository).save(testAccount);

            ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(txCaptor.capture());
            Transaction savedTx = txCaptor.getValue();
            assertThat(savedTx.getAmount()).isEqualByComparingTo(withdrawAmount);
            assertThat(savedTx.getType()).isEqualTo("Withdrawal");
            assertThat(savedTx.getAccount()).isSameAs(testAccount);
            assertThat(savedTx.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("throws on insufficient funds")
        void throwsOnInsufficientFunds() {
            BigDecimal tooMuch = new BigDecimal("2000.00");

            assertThatThrownBy(() -> accountService.withdraw(testAccount, tooMuch))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Insufficient funds");

            // Balance unchanged
            assertThat(testAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows withdrawing exact balance (zero remaining)")
        void withdrawExactBalance() {
            accountService.withdraw(testAccount, new BigDecimal("1000.00"));

            assertThat(testAccount.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(accountRepository).save(testAccount);
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("throws when withdrawal exceeds balance by a penny")
        void throwsWhenOneOverBalance() {
            assertThatThrownBy(() -> accountService.withdraw(testAccount, new BigDecimal("1000.01")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Insufficient funds");
        }
    }

    // ─── getTransactionHistory ──────────────────────────────────────────

    @Nested
    @DisplayName("getTransactionHistory")
    class GetTransactionHistory {

        @Test
        @DisplayName("returns list of transactions for an account")
        void returnsTransactions() {
            Transaction tx1 = new Transaction(new BigDecimal("100"), "Deposit", null, testAccount);
            Transaction tx2 = new Transaction(new BigDecimal("50"), "Withdrawal", null, testAccount);
            when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(tx1, tx2));

            List<Transaction> history = accountService.getTransactionHistory(testAccount);

            assertThat(history).hasSize(2);
            assertThat(history).containsExactly(tx1, tx2);
            verify(transactionRepository).findByAccountId(1L);
        }

        @Test
        @DisplayName("returns empty list when no transactions exist")
        void returnsEmptyListWhenNoTransactions() {
            when(transactionRepository.findByAccountId(1L)).thenReturn(Collections.emptyList());

            List<Transaction> history = accountService.getTransactionHistory(testAccount);

            assertThat(history).isEmpty();
        }
    }

    // ─── loadUserByUsername ─────────────────────────────────────────────

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("returns UserDetails with correct username, password, and authorities")
        void returnsUserDetails() {
            when(accountRepository.findByUsername("john")).thenReturn(Optional.of(testAccount));

            UserDetails userDetails = accountService.loadUserByUsername("john");

            assertThat(userDetails.getUsername()).isEqualTo("john");
            assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("USER");
        }

        @Test
        @DisplayName("throws RuntimeException when user not found (via findAccountByUsername)")
        void throwsWhenUserNotFound() {
            when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.loadUserByUsername("ghost"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Account not found");
        }
    }

    // ─── authorities ────────────────────────────────────────────────────

    @Nested
    @DisplayName("authorities")
    class Authorities {

        @Test
        @DisplayName("returns collection with single USER authority")
        void returnsUserAuthority() {
            Collection<? extends GrantedAuthority> auth = accountService.authorities();

            assertThat(auth).hasSize(1);
            assertThat(auth).extracting(GrantedAuthority::getAuthority)
                    .containsExactly("USER");
        }
    }

    // ─── transferAmount ─────────────────────────────────────────────────

    @Nested
    @DisplayName("transferAmount")
    class TransferAmount {

        private Account recipientAccount;

        @BeforeEach
        void setUpRecipient() {
            recipientAccount = new Account();
            recipientAccount.setId(2L);
            recipientAccount.setUsername("jane");
            recipientAccount.setPassword("encodedPass2");
            recipientAccount.setBalance(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("transfers amount between two accounts and records two transactions")
        void transfersSuccessfully() {
            when(accountRepository.findByUsername("jane")).thenReturn(Optional.of(recipientAccount));

            accountService.transferAmount(testAccount, "jane", new BigDecimal("200.00"));

            // Sender debited
            assertThat(testAccount.getBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
            // Recipient credited
            assertThat(recipientAccount.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));

            // Both accounts saved
            verify(accountRepository).save(testAccount);
            verify(accountRepository).save(recipientAccount);

            // Two transaction records created
            ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository, times(2)).save(txCaptor.capture());
            List<Transaction> savedTxns = txCaptor.getAllValues();

            // Debit transaction
            Transaction debit = savedTxns.get(0);
            assertThat(debit.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(debit.getType()).isEqualTo("Transfer Out to jane");
            assertThat(debit.getAccount()).isSameAs(testAccount);
            assertThat(debit.getTimestamp()).isNotNull();

            // Credit transaction
            Transaction credit = savedTxns.get(1);
            assertThat(credit.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(credit.getType()).isEqualTo("Transfer In from john");
            assertThat(credit.getAccount()).isSameAs(recipientAccount);
            assertThat(credit.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("throws on insufficient funds for transfer")
        void throwsOnInsufficientFunds() {
            assertThatThrownBy(() ->
                    accountService.transferAmount(testAccount, "jane", new BigDecimal("5000.00")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Insufficient funds");

            // Balance unchanged
            assertThat(testAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when recipient not found")
        void throwsWhenRecipientNotFound() {
            when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    accountService.transferAmount(testAccount, "nonexistent", new BigDecimal("100.00")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Recipient account not found");
        }

        @Test
        @DisplayName("transfers exact full balance leaving sender at zero")
        void transferExactBalance() {
            when(accountRepository.findByUsername("jane")).thenReturn(Optional.of(recipientAccount));

            accountService.transferAmount(testAccount, "jane", new BigDecimal("1000.00"));

            assertThat(testAccount.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(recipientAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
            verify(transactionRepository, times(2)).save(any(Transaction.class));
        }

        @Test
        @DisplayName("transfer of small fractional amount")
        void transferSmallFraction() {
            when(accountRepository.findByUsername("jane")).thenReturn(Optional.of(recipientAccount));

            accountService.transferAmount(testAccount, "jane", new BigDecimal("0.01"));

            assertThat(testAccount.getBalance()).isEqualByComparingTo(new BigDecimal("999.99"));
            assertThat(recipientAccount.getBalance()).isEqualByComparingTo(new BigDecimal("500.01"));
        }
    }
}
