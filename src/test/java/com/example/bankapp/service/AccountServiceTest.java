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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
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
        testAccount.setPassword("encodedPassword");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setTransactions(new ArrayList<>());
    }

    @Test
    void findAccountByUsername_WhenAccountExists_ReturnsAccount() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        Account result = accountService.findAccountByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(accountRepository).findByUsername("testuser");
    }

    @Test
    void findAccountByUsername_WhenAccountNotFound_ThrowsException() {
        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.findAccountByUsername("nonexistent");
        });

        assertEquals("Account not found", exception.getMessage());
    }

    @Test
    void registerAccount_WhenUsernameAvailable_CreatesAccount() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(1L);
            return account;
        });

        Account result = accountService.registerAccount("newuser", "password123");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void registerAccount_WhenUsernameExists_ThrowsException() {
        when(accountRepository.findByUsername("existinguser")).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.registerAccount("existinguser", "password123");
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void deposit_AddsAmountToBalanceAndCreatesTransaction() {
        BigDecimal depositAmount = new BigDecimal("500.00");
        BigDecimal expectedBalance = new BigDecimal("1500.00");

        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.deposit(testAccount, depositAmount);

        assertEquals(expectedBalance, testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_WhenSufficientFunds_SubtractsAmountAndCreatesTransaction() {
        BigDecimal withdrawAmount = new BigDecimal("300.00");
        BigDecimal expectedBalance = new BigDecimal("700.00");

        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.withdraw(testAccount, withdrawAmount);

        assertEquals(expectedBalance, testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_WhenInsufficientFunds_ThrowsException() {
        BigDecimal withdrawAmount = new BigDecimal("2000.00");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.withdraw(testAccount, withdrawAmount);
        });

        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionHistory_ReturnsTransactionsForAccount() {
        List<Transaction> transactions = new ArrayList<>();
        Transaction transaction1 = new Transaction();
        transaction1.setId(1L);
        transaction1.setAmount(new BigDecimal("100.00"));
        transaction1.setType("Deposit");
        transactions.add(transaction1);

        when(transactionRepository.findByAccountId(1L)).thenReturn(transactions);

        List<Transaction> result = accountService.getTransactionHistory(testAccount);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Deposit", result.get(0).getType());
        verify(transactionRepository).findByAccountId(1L);
    }

    @Test
    void loadUserByUsername_WhenUserExists_ReturnsUserDetails() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        UserDetails result = accountService.loadUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertNotNull(result.getAuthorities());
    }

    @Test
    void loadUserByUsername_WhenUserNotFound_ThrowsUsernameNotFoundException() {
        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            accountService.loadUserByUsername("nonexistent");
        });
    }

    @Test
    void authorities_ReturnsUserAuthority() {
        var authorities = accountService.authorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("USER")));
    }

    @Test
    void transferAmount_WhenSufficientFunds_TransfersSuccessfully() {
        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUsername("recipient");
        toAccount.setPassword("encodedPassword");
        toAccount.setBalance(new BigDecimal("500.00"));

        BigDecimal transferAmount = new BigDecimal("200.00");

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.transferAmount(testAccount, "recipient", transferAmount);

        assertEquals(new BigDecimal("800.00"), testAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), toAccount.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transferAmount_WhenInsufficientFunds_ThrowsException() {
        BigDecimal transferAmount = new BigDecimal("2000.00");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.transferAmount(testAccount, "recipient", transferAmount);
        });

        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmount_WhenRecipientNotFound_ThrowsException() {
        BigDecimal transferAmount = new BigDecimal("200.00");

        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.transferAmount(testAccount, "nonexistent", transferAmount);
        });

        assertEquals("Recipient account not found", exception.getMessage());
    }
}
