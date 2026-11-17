package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AccountService accountService;

    @Captor
    ArgumentCaptor<Transaction> transactionCaptor;

    @Captor
    ArgumentCaptor<Account> accountCaptor;

    // ======================== deposit() ========================

    @Test
    void deposit_shouldIncreaseBalance() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100"));

        accountService.deposit(account, new BigDecimal("50"));

        assertEquals(new BigDecimal("150"), account.getBalance());
        verify(accountRepository, times(1)).save(account);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void deposit_shouldCreateTransactionRecord() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100"));

        accountService.deposit(account, new BigDecimal("50"));

        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction captured = transactionCaptor.getValue();
        assertEquals("Deposit", captured.getType());
        assertEquals(new BigDecimal("50"), captured.getAmount());
    }

    // ======================== withdraw() ========================

    @Test
    void withdraw_shouldDecreaseBalance() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100"));

        accountService.withdraw(account, new BigDecimal("30"));

        assertEquals(new BigDecimal("70"), account.getBalance());
    }

    @Test
    void withdraw_shouldThrowOnInsufficientFunds() {
        Account account = new Account();
        account.setBalance(new BigDecimal("10"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("50")));
        assertEquals("Insufficient funds", ex.getMessage());
    }

    @Test
    void withdraw_shouldCreateTransactionRecord() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100"));

        accountService.withdraw(account, new BigDecimal("30"));

        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction captured = transactionCaptor.getValue();
        assertEquals("Withdrawal", captured.getType());
    }

    // ======================== transferAmount() ========================

    @Test
    void transfer_shouldDebitSenderAndCreditRecipient() {
        Account sender = new Account();
        sender.setUsername("sender");
        sender.setBalance(new BigDecimal("100"));

        Account recipient = new Account();
        recipient.setUsername("recipient");
        recipient.setBalance(new BigDecimal("50"));

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "recipient", new BigDecimal("40"));

        assertEquals(new BigDecimal("60"), sender.getBalance());
        assertEquals(new BigDecimal("90"), recipient.getBalance());
    }

    @Test
    void transfer_shouldThrowOnInsufficientFunds() {
        Account sender = new Account();
        sender.setUsername("sender");
        sender.setBalance(new BigDecimal("10"));

        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "recipient", new BigDecimal("50")));
    }

    @Test
    void transfer_shouldThrowOnRecipientNotFound() {
        Account sender = new Account();
        sender.setUsername("sender");
        sender.setBalance(new BigDecimal("100"));

        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "unknown", new BigDecimal("10")));
        assertEquals("Recipient account not found", ex.getMessage());
    }

    @Test
    void transfer_shouldCreateTwoTransactionRecords() {
        Account sender = new Account();
        sender.setUsername("sender");
        sender.setBalance(new BigDecimal("100"));

        Account recipient = new Account();
        recipient.setUsername("recipient");
        recipient.setBalance(new BigDecimal("50"));

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "recipient", new BigDecimal("40"));

        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        List<Transaction> captured = transactionCaptor.getAllValues();
        assertEquals(2, captured.size());
        assertEquals("Transfer Out to recipient", captured.get(0).getType());
        assertEquals("Transfer In from sender", captured.get(1).getType());
    }

    // ======================== registerAccount() ========================

    @Test
    void register_shouldCreateAccountWithZeroBalance() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.registerAccount("newuser", "password");

        verify(accountRepository).save(accountCaptor.capture());
        Account saved = accountCaptor.getValue();
        assertEquals("encoded", saved.getPassword());
        assertEquals(BigDecimal.ZERO, saved.getBalance());
    }

    @Test
    void register_shouldThrowOnDuplicateUsername() {
        when(accountRepository.findByUsername("existing")).thenReturn(Optional.of(new Account()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("existing", "password"));
        assertEquals("Username already exists", ex.getMessage());
    }

    // ======================== findAccountByUsername() ========================

    @Test
    void findAccount_shouldReturnAccount() {
        Account account = new Account();
        account.setUsername("testuser");
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        Account result = accountService.findAccountByUsername("testuser");

        assertEquals("testuser", result.getUsername());
    }

    @Test
    void findAccount_shouldThrowWhenNotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("unknown"));
        assertEquals("Account not found", ex.getMessage());
    }

    // ======================== loadUserByUsername() ========================

    @Test
    void loadUser_shouldReturnUserDetails() {
        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword("encodedpass");
        account.setBalance(BigDecimal.TEN);
        account.setTransactions(Collections.emptyList());

        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        UserDetails userDetails = accountService.loadUserByUsername("testuser");

        assertEquals("testuser", userDetails.getUsername());
    }

    @Test
    void loadUser_shouldThrowWhenNotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("unknown"));
    }
}
