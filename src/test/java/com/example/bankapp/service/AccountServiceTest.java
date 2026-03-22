package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUsername("testuser");
        testAccount.setPassword("encodedpassword");
        testAccount.setBalance(new BigDecimal("1000.00"));
    }

    // --- findAccountByUsername tests ---

    @Test
    void findAccountByUsername_success() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        Account result = accountService.findAccountByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(accountRepository).findByUsername("testuser");
    }

    @Test
    void findAccountByUsername_notFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("unknown"));
        assertEquals("Account not found", exception.getMessage());
    }

    // --- registerAccount tests ---

    @Test
    void registerAccount_success() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedpassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.registerAccount("newuser", "password");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("encodedpassword", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void registerAccount_usernameAlreadyExists() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("testuser", "password"));
        assertEquals("Username already exists", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    // --- deposit tests ---

    @Test
    void deposit_success() {
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.deposit(testAccount, new BigDecimal("500.00"));

        assertEquals(new BigDecimal("1500.00"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void deposit_smallAmount() {
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.deposit(testAccount, new BigDecimal("0.01"));

        assertEquals(new BigDecimal("1000.01"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    // --- withdraw tests ---

    @Test
    void withdraw_success() {
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.withdraw(testAccount, new BigDecimal("200.00"));

        assertEquals(new BigDecimal("800.00"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_exactBalance() {
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.withdraw(testAccount, new BigDecimal("1000.00"));

        assertEquals(new BigDecimal("0.00"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
    }

    @Test
    void withdraw_insufficientFunds() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(testAccount, new BigDecimal("2000.00")));
        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // --- getTransactionHistory tests ---

    @Test
    void getTransactionHistory_success() {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction(new BigDecimal("100"), "Deposit", null, testAccount));
        transactions.add(new Transaction(new BigDecimal("50"), "Withdrawal", null, testAccount));
        when(transactionRepository.findByAccountId(1L)).thenReturn(transactions);

        List<Transaction> result = accountService.getTransactionHistory(testAccount);

        assertEquals(2, result.size());
        verify(transactionRepository).findByAccountId(1L);
    }

    @Test
    void getTransactionHistory_empty() {
        when(transactionRepository.findByAccountId(1L)).thenReturn(new ArrayList<>());

        List<Transaction> result = accountService.getTransactionHistory(testAccount);

        assertTrue(result.isEmpty());
        verify(transactionRepository).findByAccountId(1L);
    }

    // --- loadUserByUsername tests ---

    @Test
    void loadUserByUsername_success() {
        testAccount.setTransactions(new ArrayList<>());
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        UserDetails userDetails = accountService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedpassword", userDetails.getPassword());
        assertFalse(userDetails.getAuthorities().isEmpty());
    }

    @Test
    void loadUserByUsername_notFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("unknown"));
    }

    // --- authorities tests ---

    @Test
    void authorities_returnsUserAuthority() {
        Collection<? extends GrantedAuthority> authorities = accountService.authorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("USER")));
    }

    // --- transferAmount tests ---

    @Test
    void transferAmount_success() {
        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUsername("recipient");
        toAccount.setBalance(new BigDecimal("500.00"));

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.transferAmount(testAccount, "recipient", new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), testAccount.getBalance());
        assertEquals(new BigDecimal("800.00"), toAccount.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transferAmount_insufficientFunds() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(testAccount, "recipient", new BigDecimal("2000.00")));
        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmount_recipientNotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(testAccount, "unknown", new BigDecimal("100.00")));
        assertEquals("Recipient account not found", exception.getMessage());
    }

    @Test
    void transferAmount_exactBalance() {
        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUsername("recipient");
        toAccount.setBalance(new BigDecimal("0.00"));

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.transferAmount(testAccount, "recipient", new BigDecimal("1000.00"));

        assertEquals(new BigDecimal("0.00"), testAccount.getBalance());
        assertEquals(new BigDecimal("1000.00"), toAccount.getBalance());
    }
}
