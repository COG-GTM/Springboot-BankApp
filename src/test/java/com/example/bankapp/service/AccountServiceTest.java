package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

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

    private Account createAccount(String username, BigDecimal balance) {
        Account account = new Account();
        account.setUsername(username);
        account.setBalance(balance);
        account.setPassword("password");
        account.setTransactions(Collections.emptyList());
        return account;
    }

    // ── findAccountByUsername ──

    @Test
    void findAccountByUsername_success() {
        Account account = createAccount("john", new BigDecimal("500"));
        when(accountRepository.findByUsername("john")).thenReturn(Optional.of(account));

        Account result = accountService.findAccountByUsername("john");

        assertEquals(account, result);
    }

    @Test
    void findAccountByUsername_notFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("unknown"));
        assertEquals("Account not found", ex.getMessage());
    }

    // ── registerAccount ──

    @Test
    void registerAccount_success() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("rawPass")).thenReturn("encodedPass");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.registerAccount("alice", "rawPass");

        verify(accountRepository).save(any(Account.class));
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertEquals("encodedPass", result.getPassword());
    }

    @Test
    void registerAccount_duplicateUsername() {
        Account existing = createAccount("alice", BigDecimal.ZERO);
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("alice", "pass"));
        assertEquals("Username already exists", ex.getMessage());
    }

    // ── deposit ──

    @Test
    void deposit_success() {
        Account account = createAccount("bob", new BigDecimal("100"));

        accountService.deposit(account, new BigDecimal("50"));

        assertEquals(new BigDecimal("150"), account.getBalance());
        verify(accountRepository, times(1)).save(account);
        verify(transactionRepository, times(1)).save(any(Transaction.class));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction saved = txCaptor.getValue();
        assertEquals("Deposit", saved.getType());
        assertEquals(new BigDecimal("50"), saved.getAmount());
    }

    // ── withdraw ──

    @Test
    void withdraw_success() {
        Account account = createAccount("carol", new BigDecimal("100"));

        accountService.withdraw(account, new BigDecimal("50"));

        assertEquals(new BigDecimal("50"), account.getBalance());
        verify(accountRepository, times(1)).save(account);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void withdraw_insufficientFunds() {
        Account account = createAccount("carol", new BigDecimal("30"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("50")));
        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    // ── transferAmount ──

    @Test
    void transferAmount_success() {
        Account from = createAccount("sender", new BigDecimal("200"));
        Account to = createAccount("receiver", new BigDecimal("50"));
        when(accountRepository.findByUsername("receiver")).thenReturn(Optional.of(to));

        accountService.transferAmount(from, "receiver", new BigDecimal("100"));

        assertEquals(new BigDecimal("100"), from.getBalance());
        assertEquals(new BigDecimal("150"), to.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());
        java.util.List<Transaction> txns = txCaptor.getAllValues();
        assertTrue(txns.stream().anyMatch(t -> t.getType().contains("Transfer Out")));
        assertTrue(txns.stream().anyMatch(t -> t.getType().contains("Transfer In")));
    }

    @Test
    void transferAmount_insufficientFunds() {
        Account from = createAccount("sender", new BigDecimal("30"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(from, "receiver", new BigDecimal("50")));
        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmount_recipientNotFound() {
        Account from = createAccount("sender", new BigDecimal("200"));
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(from, "ghost", new BigDecimal("50")));
        assertEquals("Recipient account not found", ex.getMessage());
    }

    // ── loadUserByUsername ──

    @Test
    void loadUserByUsername_success() {
        Account account = createAccount("dave", new BigDecimal("100"));
        when(accountRepository.findByUsername("dave")).thenReturn(Optional.of(account));

        UserDetails userDetails = accountService.loadUserByUsername("dave");

        assertEquals("dave", userDetails.getUsername());
        assertEquals("password", userDetails.getPassword());
    }

    @Test
    void loadUserByUsername_notFound() {
        when(accountRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("nobody"));
    }
}
