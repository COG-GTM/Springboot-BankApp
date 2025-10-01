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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @Test
    void testFindAccountByUsername_Success() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        Account result = accountService.findAccountByUsername("testuser");

        assertEquals(testAccount, result);
        verify(accountRepository).findByUsername("testuser");
    }

    @Test
    void testFindAccountByUsername_NotFound() {
        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> accountService.findAccountByUsername("nonexistent"));

        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository).findByUsername("nonexistent");
    }

    @Test
    void testRegisterAccount_Success() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        Account result = accountService.registerAccount("newuser", "password");

        assertNotNull(result);
        verify(accountRepository).findByUsername("newuser");
        verify(passwordEncoder).encode("password");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void testRegisterAccount_UsernameExists() {
        when(accountRepository.findByUsername("existinguser")).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> accountService.registerAccount("existinguser", "password"));

        assertEquals("Username already exists", exception.getMessage());
        verify(accountRepository).findByUsername("existinguser");
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void testDeposit_Success() {
        BigDecimal depositAmount = new BigDecimal("250.00");
        BigDecimal expectedBalance = new BigDecimal("1250.00");

        accountService.deposit(testAccount, depositAmount);

        assertEquals(expectedBalance, testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testWithdraw_Success() {
        BigDecimal withdrawAmount = new BigDecimal("200.00");
        BigDecimal expectedBalance = new BigDecimal("800.00");

        accountService.withdraw(testAccount, withdrawAmount);

        assertEquals(expectedBalance, testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testWithdraw_InsufficientFunds() {
        BigDecimal withdrawAmount = new BigDecimal("1500.00");

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> accountService.withdraw(testAccount, withdrawAmount));

        assertEquals("Insufficient funds", exception.getMessage());
        assertEquals(new BigDecimal("1000.00"), testAccount.getBalance()); // Balance unchanged
        verify(accountRepository, never()).save(testAccount);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testGetTransactionHistory() {
        Transaction transaction1 = new Transaction();
        transaction1.setId(1L);
        transaction1.setAmount(new BigDecimal("100.00"));
        transaction1.setType("Deposit");

        Transaction transaction2 = new Transaction();
        transaction2.setId(2L);
        transaction2.setAmount(new BigDecimal("50.00"));
        transaction2.setType("Withdrawal");

        List<Transaction> expectedTransactions = Arrays.asList(transaction1, transaction2);
        when(transactionRepository.findByAccountId(1L)).thenReturn(expectedTransactions);

        List<Transaction> result = accountService.getTransactionHistory(testAccount);

        assertEquals(expectedTransactions, result);
        verify(transactionRepository).findByAccountId(1L);
    }

    @Test
    void testTransferAmount_Success() {
        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUsername("recipient");
        toAccount.setBalance(new BigDecimal("500.00"));

        BigDecimal transferAmount = new BigDecimal("200.00");
        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(toAccount));

        accountService.transferAmount(testAccount, "recipient", transferAmount);

        assertEquals(new BigDecimal("800.00"), testAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), toAccount.getBalance());
        verify(accountRepository).findByUsername("recipient");
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void testTransferAmount_InsufficientFunds() {
        BigDecimal transferAmount = new BigDecimal("1500.00");

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> accountService.transferAmount(testAccount, "recipient", transferAmount));

        assertEquals("Insufficient funds", exception.getMessage());
        assertEquals(new BigDecimal("1000.00"), testAccount.getBalance()); // Balance unchanged
        verify(accountRepository, never()).findByUsername("recipient");
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testTransferAmount_RecipientNotFound() {
        BigDecimal transferAmount = new BigDecimal("200.00");
        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> accountService.transferAmount(testAccount, "nonexistent", transferAmount));

        assertEquals("Recipient account not found", exception.getMessage());
        assertEquals(new BigDecimal("1000.00"), testAccount.getBalance()); // Balance unchanged
        verify(accountRepository).findByUsername("nonexistent");
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
