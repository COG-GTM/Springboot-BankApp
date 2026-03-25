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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
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
    }

    // --- findAccountByUsername tests ---

    @Test
    void findAccountByUsername_shouldReturnAccount_whenFound() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        Account result = accountService.findAccountByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
        verify(accountRepository).findByUsername("testuser");
    }

    @Test
    void findAccountByUsername_shouldThrowException_whenNotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("unknown"));

        assertEquals("Account not found", exception.getMessage());
    }

    // --- registerAccount tests ---

    @Test
    void registerAccount_shouldCreateAccount_whenUsernameNotTaken() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.registerAccount("newuser", "password123");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("encodedPassword123", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void registerAccount_shouldThrowException_whenUsernameExists() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("testuser", "password123"));

        assertEquals("Username already exists", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    // --- deposit tests ---

    @Test
    void deposit_shouldIncreaseBalance() {
        BigDecimal depositAmount = new BigDecimal("500.00");

        accountService.deposit(testAccount, depositAmount);

        assertEquals(new BigDecimal("1500.00"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void deposit_shouldCreateTransactionRecord() {
        BigDecimal depositAmount = new BigDecimal("250.00");
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        accountService.deposit(testAccount, depositAmount);

        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(depositAmount, savedTransaction.getAmount());
        assertEquals("Deposit", savedTransaction.getType());
        assertEquals(testAccount, savedTransaction.getAccount());
        assertNotNull(savedTransaction.getTimestamp());
    }

    // --- withdraw tests ---

    @Test
    void withdraw_shouldDecreaseBalance_whenSufficientFunds() {
        BigDecimal withdrawAmount = new BigDecimal("300.00");

        accountService.withdraw(testAccount, withdrawAmount);

        assertEquals(new BigDecimal("700.00"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_shouldCreateTransactionRecord() {
        BigDecimal withdrawAmount = new BigDecimal("200.00");
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        accountService.withdraw(testAccount, withdrawAmount);

        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(withdrawAmount, savedTransaction.getAmount());
        assertEquals("Withdrawal", savedTransaction.getType());
        assertEquals(testAccount, savedTransaction.getAccount());
    }

    @Test
    void withdraw_shouldThrowException_whenInsufficientFunds() {
        BigDecimal withdrawAmount = new BigDecimal("2000.00");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(testAccount, withdrawAmount));

        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // --- getTransactionHistory tests ---

    @Test
    void getTransactionHistory_shouldReturnTransactions() {
        Transaction t1 = new Transaction(new BigDecimal("100"), "Deposit", null, testAccount);
        Transaction t2 = new Transaction(new BigDecimal("50"), "Withdrawal", null, testAccount);
        when(transactionRepository.findByAccountId(1L)).thenReturn(Arrays.asList(t1, t2));

        List<Transaction> result = accountService.getTransactionHistory(testAccount);

        assertEquals(2, result.size());
        verify(transactionRepository).findByAccountId(1L);
    }

    @Test
    void getTransactionHistory_shouldReturnEmptyList_whenNoTransactions() {
        when(transactionRepository.findByAccountId(1L)).thenReturn(Collections.emptyList());

        List<Transaction> result = accountService.getTransactionHistory(testAccount);

        assertTrue(result.isEmpty());
    }

    // --- loadUserByUsername tests ---

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenFound() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        UserDetails userDetails = accountService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertFalse(userDetails.getAuthorities().isEmpty());
    }

    @Test
    void loadUserByUsername_shouldThrowException_whenNotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("unknown"));
    }

    // --- authorities tests ---

    @Test
    void authorities_shouldReturnUserAuthority() {
        var authorities = accountService.authorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("USER")));
    }

    // --- transferAmount tests ---

    @Test
    void transferAmount_shouldTransferBetweenAccounts() {
        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUsername("recipient");
        toAccount.setBalance(new BigDecimal("500.00"));

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(toAccount));

        accountService.transferAmount(testAccount, "recipient", new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), testAccount.getBalance());
        assertEquals(new BigDecimal("800.00"), toAccount.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transferAmount_shouldCreateDebitAndCreditTransactions() {
        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUsername("recipient");
        toAccount.setBalance(new BigDecimal("500.00"));

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(toAccount));
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        accountService.transferAmount(testAccount, "recipient", new BigDecimal("200.00"));

        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        List<Transaction> savedTransactions = transactionCaptor.getAllValues();

        assertEquals("Transfer Out to recipient", savedTransactions.get(0).getType());
        assertEquals(testAccount, savedTransactions.get(0).getAccount());

        assertEquals("Transfer In from testuser", savedTransactions.get(1).getType());
        assertEquals(toAccount, savedTransactions.get(1).getAccount());
    }

    @Test
    void transferAmount_shouldThrowException_whenInsufficientFunds() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(testAccount, "recipient", new BigDecimal("5000.00")));

        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transferAmount_shouldThrowException_whenRecipientNotFound() {
        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(testAccount, "nonexistent", new BigDecimal("100.00")));
    }
}
