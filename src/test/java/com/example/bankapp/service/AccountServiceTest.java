package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Arrays;
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

    // --- registerAccount ---

    @Test
    void testRegisterAccount_Success() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.registerAccount("newuser", "password");

        assertEquals("newuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void testRegisterAccount_UsernameAlreadyExists() {
        Account existing = new Account();
        existing.setUsername("existinguser");
        when(accountRepository.findByUsername("existinguser")).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("existinguser", "password"));
        assertEquals("Username already exists", ex.getMessage());
    }

    // --- deposit ---

    @Test
    void testDeposit_Success() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(BigDecimal.valueOf(100));

        accountService.deposit(account, BigDecimal.valueOf(50));

        assertEquals(0, BigDecimal.valueOf(150).compareTo(account.getBalance()));
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertEquals("Deposit", savedTx.getType());
        assertEquals(0, BigDecimal.valueOf(50).compareTo(savedTx.getAmount()));
    }

    // --- withdraw ---

    @Test
    void testWithdraw_Success() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(BigDecimal.valueOf(100));

        accountService.withdraw(account, BigDecimal.valueOf(50));

        assertEquals(0, BigDecimal.valueOf(50).compareTo(account.getBalance()));
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertEquals("Withdrawal", txCaptor.getValue().getType());
    }

    @Test
    void testWithdraw_InsufficientFunds() {
        Account account = new Account();
        account.setBalance(BigDecimal.valueOf(50));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, BigDecimal.valueOf(100)));
        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any());
    }

    // --- transferAmount ---

    @Test
    void testTransferAmount_Success() {
        Account fromAccount = new Account();
        fromAccount.setUsername("sender");
        fromAccount.setBalance(BigDecimal.valueOf(200));

        Account toAccount = new Account();
        toAccount.setUsername("recipient");
        toAccount.setBalance(BigDecimal.valueOf(100));

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(toAccount));

        accountService.transferAmount(fromAccount, "recipient", BigDecimal.valueOf(50));

        assertEquals(0, BigDecimal.valueOf(150).compareTo(fromAccount.getBalance()));
        assertEquals(0, BigDecimal.valueOf(150).compareTo(toAccount.getBalance()));
        verify(accountRepository, times(2)).save(any(Account.class));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());
        List<Transaction> transactions = txCaptor.getAllValues();
        assertEquals("Transfer Out to recipient", transactions.get(0).getType());
        assertEquals("Transfer In from sender", transactions.get(1).getType());
    }

    @Test
    void testTransferAmount_InsufficientFunds() {
        Account fromAccount = new Account();
        fromAccount.setBalance(BigDecimal.valueOf(10));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(fromAccount, "recipient", BigDecimal.valueOf(100)));
        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void testTransferAmount_RecipientNotFound() {
        Account fromAccount = new Account();
        fromAccount.setBalance(BigDecimal.valueOf(200));

        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(fromAccount, "nonexistent", BigDecimal.valueOf(50)));
        assertEquals("Recipient account not found", ex.getMessage());
    }

    // --- findAccountByUsername ---

    @Test
    void testFindAccountByUsername_Success() {
        Account account = new Account();
        account.setUsername("testuser");
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        Account result = accountService.findAccountByUsername("testuser");

        assertEquals("testuser", result.getUsername());
    }

    @Test
    void testFindAccountByUsername_NotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("unknown"));
        assertEquals("Account not found", ex.getMessage());
    }

    // --- loadUserByUsername ---

    @Test
    void testLoadUserByUsername_Success() {
        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword("encodedPass");
        account.setBalance(BigDecimal.TEN);

        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        UserDetails userDetails = accountService.loadUserByUsername("testuser");

        assertEquals("testuser", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("USER")));
    }

    @Test
    void testLoadUserByUsername_NotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("unknown"));
    }

    // --- getTransactionHistory ---

    @Test
    void testGetTransactionHistory() {
        Account account = new Account();
        account.setId(1L);

        Transaction tx1 = new Transaction();
        tx1.setType("Deposit");
        Transaction tx2 = new Transaction();
        tx2.setType("Withdrawal");
        List<Transaction> expectedTransactions = Arrays.asList(tx1, tx2);

        when(transactionRepository.findByAccountId(1L)).thenReturn(expectedTransactions);

        List<Transaction> result = accountService.getTransactionHistory(account);

        assertEquals(expectedTransactions, result);
        assertEquals(2, result.size());
    }
}
