package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private static Account account(Long id, String username, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setPassword("{hash}" + username);
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    @Test
    void findAccountByUsernameReturnsAccount() {
        Account alice = account(1L, "alice", "10.00");
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        assertThat(accountService.findAccountByUsername("alice")).isSameAs(alice);
    }

    @Test
    void findAccountByUsernameThrowsWhenMissing() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findAccountByUsername("ghost"))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Account not found");
    }

    @Test
    void registerAccountEncodesPasswordAndStartsAtZeroBalance() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("ENCODED");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.registerAccount("alice", "secret");

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(saved.capture());
        assertThat(result).isSameAs(saved.getValue());
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getPassword()).isEqualTo("ENCODED");
        assertThat(result.getBalance()).isSameAs(BigDecimal.ZERO);
        assertThat(result.getId()).isNull();
        assertThat(result.getTransactions()).isNull();
        assertThat(result.getAuthorities()).isNull();
    }

    @Test
    void registerAccountRejectsDuplicateUsername() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(account(1L, "alice", "0")));

        assertThatThrownBy(() -> accountService.registerAccount("alice", "secret"))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Username already exists");
        verify(accountRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void depositAddsAmountAndRecordsDepositTransaction() {
        Account alice = account(1L, "alice", "100.00");

        accountService.deposit(alice, new BigDecimal("25.50"));

        assertThat(alice.getBalance()).isEqualByComparingTo("125.50");
        InOrder order = inOrder(accountRepository, transactionRepository);
        order.verify(accountRepository).save(alice);
        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        order.verify(transactionRepository).save(tx.capture());
        assertThat(tx.getValue().getType()).isEqualTo("Deposit");
        assertThat(tx.getValue().getAmount()).isEqualByComparingTo("25.50");
        assertThat(tx.getValue().getAccount()).isSameAs(alice);
        assertThat(tx.getValue().getTimestamp()).isNotNull();
    }

    @Test
    void depositAcceptsNegativeAmountAsIs() {
        Account alice = account(1L, "alice", "100.00");

        accountService.deposit(alice, new BigDecimal("-40.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("60.00");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdrawSubtractsAmountAndRecordsWithdrawalTransaction() {
        Account alice = account(1L, "alice", "100.00");

        accountService.withdraw(alice, new BigDecimal("30.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("70.00");
        InOrder order = inOrder(accountRepository, transactionRepository);
        order.verify(accountRepository).save(alice);
        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        order.verify(transactionRepository).save(tx.capture());
        assertThat(tx.getValue().getType()).isEqualTo("Withdrawal");
        assertThat(tx.getValue().getAmount()).isEqualByComparingTo("30.00");
        assertThat(tx.getValue().getAccount()).isSameAs(alice);
        assertThat(tx.getValue().getTimestamp()).isNotNull();
    }

    @Test
    void withdrawOfEntireBalanceIsAllowed() {
        Account alice = account(1L, "alice", "50.00");

        accountService.withdraw(alice, new BigDecimal("50.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void withdrawRejectsInsufficientFundsWithoutSaving() {
        Account alice = account(1L, "alice", "50.00");

        assertThatThrownBy(() -> accountService.withdraw(alice, new BigDecimal("50.01")))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");
        assertThat(alice.getBalance()).isEqualByComparingTo("50.00");
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdrawAcceptsNegativeAmountAsIs() {
        Account alice = account(1L, "alice", "10.00");

        accountService.withdraw(alice, new BigDecimal("-5.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("15.00");
    }

    @Test
    void transferMovesFundsAndRecordsBothSides() {
        Account alice = account(1L, "alice", "100.00");
        Account bob = account(2L, "bob", "5.00");
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        accountService.transferAmount(alice, "bob", new BigDecimal("40.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("60.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("45.00");

        InOrder order = inOrder(accountRepository, transactionRepository);
        order.verify(accountRepository).save(alice);
        order.verify(accountRepository).save(bob);
        ArgumentCaptor<Transaction> tx = ArgumentCaptor.forClass(Transaction.class);
        order.verify(transactionRepository, times(2)).save(tx.capture());

        Transaction debit = tx.getAllValues().get(0);
        assertThat(debit.getType()).isEqualTo("Transfer Out to bob");
        assertThat(debit.getAmount()).isEqualByComparingTo("40.00");
        assertThat(debit.getAccount()).isSameAs(alice);
        assertThat(debit.getTimestamp()).isNotNull();

        Transaction credit = tx.getAllValues().get(1);
        assertThat(credit.getType()).isEqualTo("Transfer In from alice");
        assertThat(credit.getAmount()).isEqualByComparingTo("40.00");
        assertThat(credit.getAccount()).isSameAs(bob);
        assertThat(credit.getTimestamp()).isNotNull();
    }

    @Test
    void transferChecksFundsBeforeLookingUpRecipient() {
        Account alice = account(1L, "alice", "10.00");

        assertThatThrownBy(() -> accountService.transferAmount(alice, "ghost", new BigDecimal("10.01")))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");
        verify(accountRepository, never()).findByUsername(any());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferRejectsUnknownRecipientWithoutSaving() {
        Account alice = account(1L, "alice", "100.00");
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transferAmount(alice, "ghost", new BigDecimal("10.00")))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Recipient account not found");
        assertThat(alice.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferAcceptsNegativeAmountAsIs() {
        Account alice = account(1L, "alice", "10.00");
        Account bob = account(2L, "bob", "10.00");
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        accountService.transferAmount(alice, "bob", new BigDecimal("-5.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("15.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("5.00");
    }

    @Test
    void getTransactionHistoryQueriesByAccountId() {
        Account alice = account(7L, "alice", "0");
        List<Transaction> history = new ArrayList<>();
        history.add(new Transaction(BigDecimal.ONE, "Deposit", null, alice));
        when(transactionRepository.findByAccountId(7L)).thenReturn(history);

        assertThat(accountService.getTransactionHistory(alice)).isSameAs(history);
    }

    @Test
    void loadUserByUsernameReturnsCopyWithUserAuthority() {
        Account stored = account(3L, "alice", "42.00");
        List<Transaction> txs = Collections.emptyList();
        stored.setTransactions(txs);
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(stored));

        UserDetails details = accountService.loadUserByUsername("alice");

        assertThat(details).isInstanceOf(Account.class).isNotSameAs(stored);
        Account loaded = (Account) details;
        assertThat(loaded.getUsername()).isEqualTo("alice");
        assertThat(loaded.getPassword()).isEqualTo("{hash}alice");
        assertThat(loaded.getBalance()).isEqualByComparingTo("42.00");
        assertThat(loaded.getTransactions()).isSameAs(txs);
        assertThat(loaded.getId()).isNull();
        assertThat(loaded.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("USER");
        assertThat(loaded.isEnabled()).isTrue();
        assertThat(loaded.isAccountNonLocked()).isTrue();
        assertThat(loaded.isAccountNonExpired()).isTrue();
        assertThat(loaded.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void loadUserByUsernameThrowsPlainRuntimeExceptionWhenMissing() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.loadUserByUsername("ghost"))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Account not found");
    }

    @Test
    void authoritiesIsSingleUserAuthorityWithoutRolePrefix() {
        assertThat(accountService.authorities()).extracting(GrantedAuthority::getAuthority).containsExactly("USER");
    }
}
