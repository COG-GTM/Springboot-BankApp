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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collection;
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
        testAccount.setUsername("john");
        testAccount.setPassword("encodedPassword");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setTransactions(Collections.emptyList());
    }

    // ── findAccountByUsername ────────────────────────────────────────────

    @Test
    void findAccountByUsername_existingUser_returnsAccount() {
        when(accountRepository.findByUsername("john")).thenReturn(Optional.of(testAccount));

        Account result = accountService.findAccountByUsername("john");

        assertEquals("john", result.getUsername());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
        verify(accountRepository).findByUsername("john");
    }

    @Test
    void findAccountByUsername_nonExistentUser_throwsException() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("unknown"));

        assertEquals("Account not found", ex.getMessage());
    }

    // ── registerAccount ─────────────────────────────────────────────────

    @Test
    void registerAccount_newUser_createsAccountWithZeroBalance() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("rawpass")).thenReturn("encodedpass");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.registerAccount("newuser", "rawpass");

        assertEquals("newuser", result.getUsername());
        assertEquals("encodedpass", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(passwordEncoder).encode("rawpass");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void registerAccount_duplicateUsername_throwsException() {
        when(accountRepository.findByUsername("john")).thenReturn(Optional.of(testAccount));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("john", "pass"));

        assertEquals("Username already exists", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    // ── deposit ─────────────────────────────────────────────────────────

    @Test
    void deposit_addsAmountToBalance() {
        BigDecimal depositAmount = new BigDecimal("500.00");

        accountService.deposit(testAccount, depositAmount);

        assertEquals(new BigDecimal("1500.00"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertEquals(depositAmount, savedTx.getAmount());
        assertEquals("Deposit", savedTx.getType());
        assertNotNull(savedTx.getTimestamp());
        assertEquals(testAccount, savedTx.getAccount());
    }

    // ── withdraw ────────────────────────────────────────────────────────

    @Test
    void withdraw_sufficientFunds_subtractsAmount() {
        BigDecimal withdrawAmount = new BigDecimal("300.00");

        accountService.withdraw(testAccount, withdrawAmount);

        assertEquals(new BigDecimal("700.00"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertEquals(withdrawAmount, savedTx.getAmount());
        assertEquals("Withdrawal", savedTx.getType());
        assertNotNull(savedTx.getTimestamp());
        assertEquals(testAccount, savedTx.getAccount());
    }

    @Test
    void withdraw_insufficientFunds_throwsException() {
        BigDecimal withdrawAmount = new BigDecimal("2000.00");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(testAccount, withdrawAmount));

        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // ── getTransactionHistory ───────────────────────────────────────────

    @Test
    void getTransactionHistory_returnsList() {
        Transaction tx1 = new Transaction(new BigDecimal("100"), "Deposit", null, testAccount);
        Transaction tx2 = new Transaction(new BigDecimal("50"), "Withdrawal", null, testAccount);
        List<Transaction> expectedTxs = List.of(tx1, tx2);

        when(transactionRepository.findByAccountId(1L)).thenReturn(expectedTxs);

        List<Transaction> result = accountService.getTransactionHistory(testAccount);

        assertEquals(2, result.size());
        assertEquals(expectedTxs, result);
        verify(transactionRepository).findByAccountId(1L);
    }

    // ── loadUserByUsername ──────────────────────────────────────────────

    @Test
    void loadUserByUsername_existingUser_returnsUserDetails() {
        when(accountRepository.findByUsername("john")).thenReturn(Optional.of(testAccount));

        UserDetails userDetails = accountService.loadUserByUsername("john");

        assertEquals("john", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("USER")));
    }

    @Test
    void loadUserByUsername_nonExistentUser_throwsException() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("unknown"));
    }

    // ── authorities ─────────────────────────────────────────────────────

    @Test
    void authorities_returnsUserRole() {
        Collection<? extends GrantedAuthority> authorities = accountService.authorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("USER")));
    }

    // ── transferAmount ──────────────────────────────────────────────────

    @Test
    void transferAmount_sufficientFunds_transfersCorrectly() {
        Account recipient = new Account();
        recipient.setId(2L);
        recipient.setUsername("jane");
        recipient.setPassword("pass");
        recipient.setBalance(new BigDecimal("200.00"));

        when(accountRepository.findByUsername("jane")).thenReturn(Optional.of(recipient));

        BigDecimal transferAmount = new BigDecimal("400.00");
        accountService.transferAmount(testAccount, "jane", transferAmount);

        // Verify balances
        assertEquals(new BigDecimal("600.00"), testAccount.getBalance());
        assertEquals(new BigDecimal("600.00"), recipient.getBalance());

        // Verify both accounts saved
        verify(accountRepository).save(testAccount);
        verify(accountRepository).save(recipient);

        // Verify two transactions saved
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());
        List<Transaction> savedTxs = txCaptor.getAllValues();

        Transaction debit = savedTxs.get(0);
        assertEquals(transferAmount, debit.getAmount());
        assertEquals("Transfer Out to jane", debit.getType());
        assertEquals(testAccount, debit.getAccount());

        Transaction credit = savedTxs.get(1);
        assertEquals(transferAmount, credit.getAmount());
        assertEquals("Transfer In from john", credit.getType());
        assertEquals(recipient, credit.getAccount());
    }

    @Test
    void transferAmount_insufficientFunds_throwsException() {
        BigDecimal transferAmount = new BigDecimal("5000.00");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(testAccount, "jane", transferAmount));

        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmount_recipientNotFound_throwsException() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        BigDecimal transferAmount = new BigDecimal("100.00");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(testAccount, "ghost", transferAmount));

        assertEquals("Recipient account not found", ex.getMessage());
    }
}
