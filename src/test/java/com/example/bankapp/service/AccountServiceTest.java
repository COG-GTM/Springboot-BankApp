package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
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

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setUsername("alice");
        account.setPassword("encoded");
        account.setBalance(new BigDecimal("100.00"));
    }

    @Test
    void findAccountByUsernameReturnsAccount() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(account));

        assertThat(accountService.findAccountByUsername("alice")).isSameAs(account);
    }

    @Test
    void findAccountByUsernameThrowsWhenMissing() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findAccountByUsername("ghost"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account not found");
    }

    @Test
    void registerAccountEncodesPasswordAndStartsAtZeroBalance() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account created = accountService.registerAccount("bob", "secret");

        assertThat(created.getUsername()).isEqualTo("bob");
        assertThat(created.getPassword()).isEqualTo("hashed");
        assertThat(created.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void registerAccountRejectsDuplicateUsername() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.registerAccount("alice", "secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username already exists");

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void depositIncreasesBalanceAndRecordsTransaction() {
        accountService.deposit(account, new BigDecimal("25.50"));

        assertThat(account.getBalance()).isEqualByComparingTo("125.50");
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("Deposit");
        assertThat(saved.getAmount()).isEqualByComparingTo("25.50");
        assertThat(saved.getAccount()).isSameAs(account);
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void withdrawDecreasesBalanceAndRecordsTransaction() {
        accountService.withdraw(account, new BigDecimal("40.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("60.00");
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("Withdrawal");
    }

    @Test
    void withdrawOfExactBalanceIsAllowed() {
        accountService.withdraw(account, new BigDecimal("100.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void withdrawRejectsInsufficientFunds() {
        assertThatThrownBy(() -> accountService.withdraw(account, new BigDecimal("100.01")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionHistoryDelegatesToRepository() {
        List<Transaction> transactions = Collections.singletonList(
                new Transaction(BigDecimal.TEN, "Deposit", LocalDateTime.now(), account));
        when(transactionRepository.findByAccountId(1L)).thenReturn(transactions);

        assertThat(accountService.getTransactionHistory(account)).isEqualTo(transactions);
    }

    @Test
    void loadUserByUsernameReturnsUserDetailsWithAuthority() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(account));

        UserDetails details = accountService.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("USER");
    }

    @Test
    void loadUserByUsernameThrowsForUnknownUser() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.loadUserByUsername("ghost"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account not found");
    }

    @Test
    void authoritiesContainsSingleUserRole() {
        Collection<? extends GrantedAuthority> authorities = accountService.authorities();

        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("USER");
    }

    @Test
    void transferMovesFundsAndRecordsBothLegs() {
        Account recipient = new Account();
        recipient.setId(2L);
        recipient.setUsername("bob");
        recipient.setBalance(new BigDecimal("10.00"));
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(account, "bob", new BigDecimal("30.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("40.00");
        verify(accountRepository).save(account);
        verify(accountRepository).save(recipient);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        List<Transaction> legs = captor.getAllValues();
        assertThat(legs.get(0).getType()).isEqualTo("Transfer Out to bob");
        assertThat(legs.get(0).getAccount()).isSameAs(account);
        assertThat(legs.get(1).getType()).isEqualTo("Transfer In from alice");
        assertThat(legs.get(1).getAccount()).isSameAs(recipient);
    }

    @Test
    void transferRejectsInsufficientFunds() {
        assertThatThrownBy(() -> accountService.transferAmount(account, "bob", new BigDecimal("500.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");

        verify(accountRepository, never()).findByUsername("bob");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferRejectsUnknownRecipient() {
        when(accountRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transferAmount(account, "nobody", new BigDecimal("10.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Recipient account not found");

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transferToSelfLeavesBalanceUnchanged() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(account));

        accountService.transferAmount(account, "alice", new BigDecimal("25.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void depositAcceptsZeroAmountWithoutChangingBalance() {
        accountService.deposit(account, BigDecimal.ZERO);

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void transactionHistoryIsEmptyForAccountWithoutTransactions() {
        when(transactionRepository.findByAccountId(1L)).thenReturn(Arrays.asList());

        assertThat(accountService.getTransactionHistory(account)).isEmpty();
    }
}
