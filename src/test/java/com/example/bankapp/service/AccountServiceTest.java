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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        account.setPassword("encodedPassword");
        account.setBalance(new BigDecimal("1000.00"));
        account.setTransactions(Collections.emptyList());
    }

    @Test
    void findAccountByUsername_returnsAccount_whenUsernameExists() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        Account result = accountService.findAccountByUsername("testuser");

        assertEquals("testuser", result.getUsername());
        verify(accountRepository).findByUsername("testuser");
    }

    @Test
    void findAccountByUsername_throwsException_whenUsernameNotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("unknown"));

        assertEquals("Account not found", exception.getMessage());
    }

    @Test
    void registerAccount_createsAccount_whenUsernameIsNew() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.registerAccount("newuser", "rawPassword");

        assertEquals("newuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void registerAccount_throwsException_whenUsernameAlreadyExists() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("testuser", "password"));

        assertEquals("Username already exists", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void deposit_addsAmountAndCreatesTransaction() {
        BigDecimal depositAmount = new BigDecimal("500.00");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.deposit(account, depositAmount);

        assertEquals(new BigDecimal("1500.00"), account.getBalance());
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertEquals(depositAmount, savedTx.getAmount());
        assertEquals("Deposit", savedTx.getType());
        assertEquals(account, savedTx.getAccount());
    }

    @Test
    void withdraw_subtractsAmountAndCreatesTransaction() {
        BigDecimal withdrawAmount = new BigDecimal("300.00");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.withdraw(account, withdrawAmount);

        assertEquals(new BigDecimal("700.00"), account.getBalance());
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertEquals(withdrawAmount, savedTx.getAmount());
        assertEquals("Withdrawal", savedTx.getType());
        assertEquals(account, savedTx.getAccount());
    }

    @Test
    void withdraw_throwsException_whenInsufficientFunds() {
        BigDecimal withdrawAmount = new BigDecimal("2000.00");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, withdrawAmount));

        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionHistory_returnsTransactions() {
        Transaction tx = new Transaction(new BigDecimal("100.00"), "Deposit", null, account);
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(tx));

        List<Transaction> result = accountService.getTransactionHistory(account);

        assertEquals(1, result.size());
        assertEquals("Deposit", result.get(0).getType());
        verify(transactionRepository).findByAccountId(1L);
    }

    @Test
    void loadUserByUsername_returnsUserDetails_whenUsernameExists() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        UserDetails userDetails = accountService.loadUserByUsername("testuser");

        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertFalse(userDetails.getAuthorities().isEmpty());
    }

    @Test
    void loadUserByUsername_throwsException_whenUsernameNotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("unknown"));
    }

    @Test
    void transferAmount_movesBalanceAndCreatesTwoTransactions() {
        Account recipient = new Account();
        recipient.setId(2L);
        recipient.setUsername("recipient");
        recipient.setPassword("pass");
        recipient.setBalance(new BigDecimal("200.00"));

        BigDecimal transferAmount = new BigDecimal("300.00");

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(recipient));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.transferAmount(account, "recipient", transferAmount);

        assertEquals(new BigDecimal("700.00"), account.getBalance());
        assertEquals(new BigDecimal("500.00"), recipient.getBalance());

        verify(accountRepository, times(2)).save(any(Account.class));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());
        List<Transaction> savedTxs = txCaptor.getAllValues();

        assertEquals("Transfer Out to recipient", savedTxs.get(0).getType());
        assertEquals(account, savedTxs.get(0).getAccount());
        assertEquals("Transfer In from testuser", savedTxs.get(1).getType());
        assertEquals(recipient, savedTxs.get(1).getAccount());
    }

    @Test
    void transferAmount_throwsException_whenInsufficientFunds() {
        BigDecimal transferAmount = new BigDecimal("5000.00");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(account, "recipient", transferAmount));

        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmount_throwsException_whenRecipientNotFound() {
        BigDecimal transferAmount = new BigDecimal("100.00");
        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(account, "nonexistent", transferAmount));

        assertEquals("Recipient account not found", exception.getMessage());
    }
}
