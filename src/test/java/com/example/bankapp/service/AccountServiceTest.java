package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

    // --- findAccountByUsername ---

    @Test
    void findAccountByUsername_Success() {
        Account account = new Account();
        account.setUsername("testuser");
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        Account result = accountService.findAccountByUsername("testuser");

        assertEquals("testuser", result.getUsername());
        verify(accountRepository).findByUsername("testuser");
    }

    @Test
    void findAccountByUsername_NotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("unknown"));
        assertEquals("Account not found", ex.getMessage());
        verify(accountRepository).findByUsername("unknown");
    }

    // --- registerAccount ---

    @Test
    void registerAccount_Success() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account a = invocation.getArgument(0);
            a.setId(1L);
            return a;
        });

        Account result = accountService.registerAccount("newuser", "password");

        assertEquals("newuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).findByUsername("newuser");
        verify(passwordEncoder).encode("password");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void registerAccount_UsernameAlreadyExists() {
        Account existing = new Account();
        existing.setUsername("existinguser");
        when(accountRepository.findByUsername("existinguser")).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("existinguser", "password"));
        assertEquals("Username already exists", ex.getMessage());
        verify(accountRepository).findByUsername("existinguser");
        verify(accountRepository, never()).save(any(Account.class));
    }

    // --- deposit ---

    @Test
    void deposit_Success() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("100.00"));

        accountService.deposit(account, new BigDecimal("50.00"));

        assertEquals(new BigDecimal("150.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void deposit_ZeroBalance() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(BigDecimal.ZERO);

        accountService.deposit(account, new BigDecimal("200.00"));

        assertEquals(new BigDecimal("200.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    // --- withdraw ---

    @Test
    void withdraw_Success() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("200.00"));

        accountService.withdraw(account, new BigDecimal("50.00"));

        assertEquals(new BigDecimal("150.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_ExactBalance() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("100.00"));

        accountService.withdraw(account, new BigDecimal("100.00"));

        assertEquals(new BigDecimal("0.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_InsufficientFunds() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("30.00"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("50.00")));
        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // --- getTransactionHistory ---

    @Test
    void getTransactionHistory_Success() {
        Account account = new Account();
        account.setId(1L);
        when(transactionRepository.findByAccountId(1L)).thenReturn(java.util.List.of());

        var result = accountService.getTransactionHistory(account);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(transactionRepository).findByAccountId(1L);
    }

    // --- loadUserByUsername ---

    @Test
    void loadUserByUsername_Success() {
        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword("encodedPass");
        account.setBalance(new BigDecimal("500.00"));
        account.setTransactions(null);
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        UserDetails result = accountService.loadUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPass", result.getPassword());
        assertFalse(result.getAuthorities().isEmpty());
    }

    @Test
    void loadUserByUsername_NotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("unknown"));
    }

    // --- authorities ---

    @Test
    void authorities_ReturnsUserAuthority() {
        Collection<? extends GrantedAuthority> authorities = accountService.authorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("USER", authorities.iterator().next().getAuthority());
    }

    // --- transferAmount ---

    @Test
    void transferAmount_Success() {
        Account fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setUsername("sender");
        fromAccount.setBalance(new BigDecimal("500.00"));

        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUsername("receiver");
        toAccount.setBalance(new BigDecimal("100.00"));

        when(accountRepository.findByUsername("receiver")).thenReturn(Optional.of(toAccount));

        accountService.transferAmount(fromAccount, "receiver", new BigDecimal("200.00"));

        assertEquals(new BigDecimal("300.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("300.00"), toAccount.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transferAmount_InsufficientFunds() {
        Account fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setUsername("sender");
        fromAccount.setBalance(new BigDecimal("50.00"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(fromAccount, "receiver", new BigDecimal("100.00")));
        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmount_RecipientNotFound() {
        Account fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setUsername("sender");
        fromAccount.setBalance(new BigDecimal("500.00"));

        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(fromAccount, "unknown", new BigDecimal("100.00")));
        assertEquals("Recipient account not found", ex.getMessage());
    }

    @Test
    void transferAmount_ExactBalance() {
        Account fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setUsername("sender");
        fromAccount.setBalance(new BigDecimal("100.00"));

        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUsername("receiver");
        toAccount.setBalance(new BigDecimal("50.00"));

        when(accountRepository.findByUsername("receiver")).thenReturn(Optional.of(toAccount));

        accountService.transferAmount(fromAccount, "receiver", new BigDecimal("100.00"));

        assertEquals(new BigDecimal("0.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("150.00"), toAccount.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }
}
