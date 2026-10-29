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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    private Account alice;
    private Account bob;

    @BeforeEach
    void setUp() {
        alice = account(1L, "alice", "100.00");
        bob = account(2L, "bob", "20.00");
    }

    @Test
    void findAccountByUsernameReturnsAccount() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        assertThat(accountService.findAccountByUsername("alice")).isSameAs(alice);
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
        when(accountRepository.findByUsername("carol")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account saved = accountService.registerAccount("carol", "secret");

        assertThat(saved.getUsername()).isEqualTo("carol");
        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void registerAccountRejectsDuplicateUsername() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> accountService.registerAccount("alice", "secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username already exists");
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void depositAddsToBalanceAndRecordsTransaction() {
        accountService.deposit(alice, new BigDecimal("50.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("150.00");
        verify(accountRepository).save(alice);
        Transaction tx = captureSingleTransaction();
        assertThat(tx.getType()).isEqualTo("Deposit");
        assertThat(tx.getAmount()).isEqualByComparingTo("50.00");
        assertThat(tx.getAccount()).isSameAs(alice);
        assertThat(tx.getTimestamp()).isNotNull();
    }

    @Test
    void withdrawSubtractsFromBalanceAndRecordsTransaction() {
        accountService.withdraw(alice, new BigDecimal("30.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("70.00");
        verify(accountRepository).save(alice);
        Transaction tx = captureSingleTransaction();
        assertThat(tx.getType()).isEqualTo("Withdrawal");
        assertThat(tx.getAmount()).isEqualByComparingTo("30.00");
        assertThat(tx.getAccount()).isSameAs(alice);
    }

    @Test
    void withdrawAllowsEmptyingTheAccount() {
        accountService.withdraw(alice, new BigDecimal("100.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void withdrawRejectsInsufficientFundsWithoutSaving() {
        assertThatThrownBy(() -> accountService.withdraw(alice, new BigDecimal("100.01")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");

        assertThat(alice.getBalance()).isEqualByComparingTo("100.00");
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void transferMovesFundsAndRecordsBothSides() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        accountService.transferAmount(alice, "bob", new BigDecimal("40.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("60.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("60.00");
        verify(accountRepository).save(alice);
        verify(accountRepository).save(bob);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        List<Transaction> txs = captor.getAllValues();
        assertThat(txs.get(0).getType()).isEqualTo("Transfer Out to bob");
        assertThat(txs.get(0).getAccount()).isSameAs(alice);
        assertThat(txs.get(1).getType()).isEqualTo("Transfer In from alice");
        assertThat(txs.get(1).getAccount()).isSameAs(bob);
        assertThat(txs).allSatisfy(tx -> assertThat(tx.getAmount()).isEqualByComparingTo("40.00"));
    }

    @Test
    void transferRejectsInsufficientFundsBeforeLookingUpRecipient() {
        assertThatThrownBy(() -> accountService.transferAmount(alice, "bob", new BigDecimal("500.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");

        verify(accountRepository, never()).findByUsername(anyString());
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void transferRejectsUnknownRecipientWithoutMovingFunds() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transferAmount(alice, "ghost", new BigDecimal("10.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Recipient account not found");

        assertThat(alice.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void getTransactionHistoryQueriesByAccountId() {
        List<Transaction> history = List.of(new Transaction());
        when(transactionRepository.findByAccountId(1L)).thenReturn(history);

        assertThat(accountService.getTransactionHistory(alice)).isSameAs(history);
    }

    @Test
    void loadUserByUsernameReturnsUserWithUserAuthority() {
        alice.setPassword("hashed");
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        UserDetails user = accountService.loadUserByUsername("alice");

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getPassword()).isEqualTo("hashed");
        assertThat(user.getAuthorities()).extracting("authority").containsExactly("USER");
    }

    private Transaction captureSingleTransaction() {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Account account(Long id, String username, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setBalance(new BigDecimal(balance));
        return account;
    }
}
