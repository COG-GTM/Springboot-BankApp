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

    // ---- deposit() tests ----

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

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertEquals("Deposit", saved.getType());
        assertEquals(new BigDecimal("50"), saved.getAmount());
    }

    // ---- withdraw() tests ----

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

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertEquals("Withdrawal", saved.getType());
    }

    // ---- transferAmount() tests ----

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

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "recipient", new BigDecimal("50")));
        assertEquals("Insufficient funds", ex.getMessage());
    }

    @Test
    void transfer_shouldThrowOnRecipientNotFound() {
        Account sender = new Account();
        sender.setUsername("sender");
        sender.setBalance(new BigDecimal("100"));

        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "unknown", new BigDecimal("40")));
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

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());

        Transaction debit = captor.getAllValues().get(0);
        Transaction credit = captor.getAllValues().get(1);

        assertEquals("Transfer Out to recipient", debit.getType());
        assertEquals("Transfer In from sender", credit.getType());
    }

    // ---- registerAccount() tests ----

    @Test
    void register_shouldCreateAccountWithZeroBalance() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.registerAccount("newuser", "password");

        assertEquals("encoded", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void register_shouldThrowOnDuplicateUsername() {
        when(accountRepository.findByUsername("existing")).thenReturn(Optional.of(new Account()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("existing", "password"));
        assertEquals("Username already exists", ex.getMessage());
    }

    // ---- findAccountByUsername() tests ----

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

    // ---- loadUserByUsername() tests ----

    @Test
    void loadUser_shouldReturnUserDetails() {
        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword("encoded");
        account.setBalance(BigDecimal.TEN);
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        UserDetails userDetails = accountService.loadUserByUsername("testuser");

        assertEquals("testuser", userDetails.getUsername());
        assertNotNull(userDetails.getAuthorities());
        assertFalse(userDetails.getAuthorities().isEmpty());
    }

    @Test
    void loadUser_shouldThrowWhenNotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("unknown"));
    }
}
