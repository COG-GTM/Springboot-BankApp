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
        testAccount.setPassword("encodedPassword");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setTransactions(new ArrayList<>());
    }

    @Test
    void findAccountByUsername_Success() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        Account result = accountService.findAccountByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
        verify(accountRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void findAccountByUsername_NotFound() {
        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.findAccountByUsername("nonexistent");
        });

        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    void registerAccount_Success() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.setId(2L);
            return savedAccount;
        });

        Account result = accountService.registerAccount("newuser", "password123");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository, times(1)).findByUsername("newuser");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void registerAccount_UsernameAlreadyExists() {
        when(accountRepository.findByUsername("existinguser")).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.registerAccount("existinguser", "password123");
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(accountRepository, times(1)).findByUsername("existinguser");
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void deposit_Success() {
        BigDecimal depositAmount = new BigDecimal("500.00");
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.deposit(testAccount, depositAmount);

        assertEquals(new BigDecimal("1500.00"), testAccount.getBalance());
        verify(accountRepository, times(1)).save(testAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void deposit_VerifyTransactionCreated() {
        BigDecimal depositAmount = new BigDecimal("250.00");
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction savedTransaction = invocation.getArgument(0);
            assertEquals(depositAmount, savedTransaction.getAmount());
            assertEquals("Deposit", savedTransaction.getType());
            assertEquals(testAccount, savedTransaction.getAccount());
            assertNotNull(savedTransaction.getTimestamp());
            return savedTransaction;
        });

        accountService.deposit(testAccount, depositAmount);

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void withdraw_Success() {
        BigDecimal withdrawAmount = new BigDecimal("300.00");
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.withdraw(testAccount, withdrawAmount);

        assertEquals(new BigDecimal("700.00"), testAccount.getBalance());
        verify(accountRepository, times(1)).save(testAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void withdraw_InsufficientFunds() {
        BigDecimal withdrawAmount = new BigDecimal("1500.00");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.withdraw(testAccount, withdrawAmount);
        });

        assertEquals("Insufficient funds", exception.getMessage());
        assertEquals(new BigDecimal("1000.00"), testAccount.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void withdraw_ExactBalance() {
        BigDecimal withdrawAmount = new BigDecimal("1000.00");
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.withdraw(testAccount, withdrawAmount);

        assertEquals(BigDecimal.ZERO.setScale(2), testAccount.getBalance().setScale(2));
        verify(accountRepository, times(1)).save(testAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void withdraw_VerifyTransactionCreated() {
        BigDecimal withdrawAmount = new BigDecimal("200.00");
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction savedTransaction = invocation.getArgument(0);
            assertEquals(withdrawAmount, savedTransaction.getAmount());
            assertEquals("Withdrawal", savedTransaction.getType());
            assertEquals(testAccount, savedTransaction.getAccount());
            assertNotNull(savedTransaction.getTimestamp());
            return savedTransaction;
        });

        accountService.withdraw(testAccount, withdrawAmount);

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void getTransactionHistory_Success() {
        List<Transaction> transactions = new ArrayList<>();
        Transaction t1 = new Transaction(new BigDecimal("100.00"), "Deposit", null, testAccount);
        Transaction t2 = new Transaction(new BigDecimal("50.00"), "Withdrawal", null, testAccount);
        transactions.add(t1);
        transactions.add(t2);

        when(transactionRepository.findByAccountId(1L)).thenReturn(transactions);

        List<Transaction> result = accountService.getTransactionHistory(testAccount);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(transactionRepository, times(1)).findByAccountId(1L);
    }

    @Test
    void getTransactionHistory_Empty() {
        when(transactionRepository.findByAccountId(1L)).thenReturn(new ArrayList<>());

        List<Transaction> result = accountService.getTransactionHistory(testAccount);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(transactionRepository, times(1)).findByAccountId(1L);
    }

    @Test
    void loadUserByUsername_Success() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        UserDetails result = accountService.loadUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertNotNull(result.getAuthorities());
        verify(accountRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_NotFound() {
        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            accountService.loadUserByUsername("nonexistent");
        });

        verify(accountRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    void authorities_ReturnsUserAuthority() {
        Collection<? extends GrantedAuthority> authorities = accountService.authorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("USER")));
    }

    @Test
    void transferAmount_Success() {
        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUsername("recipient");
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
    void transferAmount_InsufficientFunds() {
        BigDecimal transferAmount = new BigDecimal("1500.00");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.transferAmount(testAccount, "recipient", transferAmount);
        });

        assertEquals("Insufficient funds", exception.getMessage());
        assertEquals(new BigDecimal("1000.00"), testAccount.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmount_RecipientNotFound() {
        BigDecimal transferAmount = new BigDecimal("200.00");

        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            accountService.transferAmount(testAccount, "nonexistent", transferAmount);
        });

        assertEquals("Recipient account not found", exception.getMessage());
        verify(accountRepository, times(1)).findByUsername("nonexistent");
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transferAmount_VerifyTransactionsCreated() {
        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUsername("recipient");
        toAccount.setBalance(new BigDecimal("500.00"));

        BigDecimal transferAmount = new BigDecimal("100.00");

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        List<Transaction> savedTransactions = new ArrayList<>();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            savedTransactions.add(t);
            return t;
        });

        accountService.transferAmount(testAccount, "recipient", transferAmount);

        assertEquals(2, savedTransactions.size());
        
        Transaction debitTransaction = savedTransactions.get(0);
        assertEquals(transferAmount, debitTransaction.getAmount());
        assertTrue(debitTransaction.getType().contains("Transfer Out"));
        assertEquals(testAccount, debitTransaction.getAccount());

        Transaction creditTransaction = savedTransactions.get(1);
        assertEquals(transferAmount, creditTransaction.getAmount());
        assertTrue(creditTransaction.getType().contains("Transfer In"));
        assertEquals(toAccount, creditTransaction.getAccount());
    }

    @Test
    void transferAmount_ExactBalance() {
        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUsername("recipient");
        toAccount.setBalance(new BigDecimal("500.00"));

        BigDecimal transferAmount = new BigDecimal("1000.00");

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.transferAmount(testAccount, "recipient", transferAmount);

        assertEquals(BigDecimal.ZERO.setScale(2), testAccount.getBalance().setScale(2));
        assertEquals(new BigDecimal("1500.00"), toAccount.getBalance());
    }
}
