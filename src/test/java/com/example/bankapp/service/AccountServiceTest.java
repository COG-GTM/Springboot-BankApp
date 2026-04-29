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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionLoggingService transactionLoggingService;

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

    @Test
    void deposit_shouldIncreaseBalance() {
        BigDecimal depositAmount = new BigDecimal("500.00");
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionLoggingService.logDepositAsync(any(Account.class), any(BigDecimal.class)))
                .thenReturn(CompletableFuture.completedFuture(new Transaction()));

        accountService.deposit(testAccount, depositAmount);

        assertEquals(new BigDecimal("1500.00"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(transactionLoggingService).logDepositAsync(testAccount, depositAmount);
    }

    @Test
    void deposit_withZeroAmount_shouldStillProcess() {
        BigDecimal depositAmount = BigDecimal.ZERO;
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionLoggingService.logDepositAsync(any(Account.class), any(BigDecimal.class)))
                .thenReturn(CompletableFuture.completedFuture(new Transaction()));

        accountService.deposit(testAccount, depositAmount);

        assertEquals(new BigDecimal("1000.00"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
    }

    @Test
    void withdraw_shouldDecreaseBalance() {
        BigDecimal withdrawAmount = new BigDecimal("300.00");
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionLoggingService.logWithdrawalAsync(any(Account.class), any(BigDecimal.class)))
                .thenReturn(CompletableFuture.completedFuture(new Transaction()));

        accountService.withdraw(testAccount, withdrawAmount);

        assertEquals(new BigDecimal("700.00"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(transactionLoggingService).logWithdrawalAsync(testAccount, withdrawAmount);
    }

    @Test
    void withdraw_withInsufficientFunds_shouldThrowException() {
        BigDecimal withdrawAmount = new BigDecimal("1500.00");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(testAccount, withdrawAmount));

        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any());
        verify(transactionLoggingService, never()).logWithdrawalAsync(any(), any());
    }

    @Test
    void transferAmount_shouldDebitSenderAndCreditRecipient() {
        Account recipientAccount = new Account();
        recipientAccount.setId(2L);
        recipientAccount.setUsername("recipient");
        recipientAccount.setBalance(new BigDecimal("500.00"));

        BigDecimal transferAmount = new BigDecimal("200.00");

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(recipientAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionLoggingService.logTransferAsync(any(Account.class), any(BigDecimal.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(new Transaction()));

        accountService.transferAmount(testAccount, "recipient", transferAmount);

        assertEquals(new BigDecimal("800.00"), testAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), recipientAccount.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionLoggingService, times(2)).logTransferAsync(any(), eq(transferAmount), anyString());
    }

    @Test
    void transferAmount_withInsufficientFunds_shouldThrowException() {
        BigDecimal transferAmount = new BigDecimal("2000.00");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(testAccount, "recipient", transferAmount));

        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void transferAmount_toNonExistentUser_shouldThrowException() {
        BigDecimal transferAmount = new BigDecimal("100.00");

        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(testAccount, "nonexistent", transferAmount));

        assertEquals("Recipient account not found", exception.getMessage());
    }

    @Test
    void registerAccount_shouldCreateAccountWithZeroBalance() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> {
            Account saved = i.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        Account account = accountService.registerAccount("newuser", "password");

        assertEquals("newuser", account.getUsername());
        assertEquals(BigDecimal.ZERO, account.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void registerAccount_withDuplicateUsername_shouldThrowException() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("testuser", "password"));

        assertEquals("Username already exists", exception.getMessage());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void findAccountByUsername_shouldReturnAccount() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        Account found = accountService.findAccountByUsername("testuser");

        assertEquals("testuser", found.getUsername());
        assertEquals(new BigDecimal("1000.00"), found.getBalance());
    }

    @Test
    void findAccountByUsername_notFound_shouldThrowException() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("unknown"));

        assertEquals("Account not found", exception.getMessage());
    }

    @Test
    void getTransactionHistory_shouldReturnTransactions() {
        Transaction t1 = new Transaction();
        t1.setId(1L);
        Transaction t2 = new Transaction();
        t2.setId(2L);

        when(transactionRepository.findByAccountId(1L)).thenReturn(Arrays.asList(t1, t2));

        List<Transaction> history = accountService.getTransactionHistory(testAccount);

        assertEquals(2, history.size());
        verify(transactionRepository).findByAccountId(1L);
    }

    @Test
    void getTransactionHistory_noTransactions_shouldReturnEmptyList() {
        when(transactionRepository.findByAccountId(1L)).thenReturn(Collections.emptyList());

        List<Transaction> history = accountService.getTransactionHistory(testAccount);

        assertTrue(history.isEmpty());
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        var userDetails = accountService.loadUserByUsername("testuser");

        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
    }
}
