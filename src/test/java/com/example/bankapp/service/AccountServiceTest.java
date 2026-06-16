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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        account.setUsername("alice");
        account.setPassword("secret");
        account.setBalance(new BigDecimal("100"));
    }

    // ----- deposit() -----

    @Test
    void deposit_shouldIncreaseBalanceByAmount() {
        accountService.deposit(account, new BigDecimal("50"));

        assertEquals(new BigDecimal("150"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void deposit_shouldCreateTransactionWithCorrectType() {
        accountService.deposit(account, new BigDecimal("50"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        Transaction saved = captor.getValue();
        assertEquals("Deposit", saved.getType());
        assertEquals(new BigDecimal("50"), saved.getAmount());
    }

    // ----- withdraw() -----

    @Test
    void withdraw_shouldDecreaseBalanceByAmount() {
        accountService.withdraw(account, new BigDecimal("30"));

        assertEquals(new BigDecimal("70"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_shouldThrowRuntimeExceptionWhenInsufficientFunds() {
        account.setBalance(new BigDecimal("10"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("50")));

        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void withdraw_shouldCreateTransactionWithCorrectType() {
        accountService.withdraw(account, new BigDecimal("30"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        Transaction saved = captor.getValue();
        assertEquals("Withdrawal", saved.getType());
        assertEquals(new BigDecimal("30"), saved.getAmount());
    }

    // ----- transferAmount() -----

    @Test
    void transferAmount_shouldDeductFromSenderAndAddToRecipient() {
        account.setBalance(new BigDecimal("200"));

        Account recipient = new Account();
        recipient.setId(2L);
        recipient.setUsername("bob");
        recipient.setBalance(new BigDecimal("0"));

        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(account, "bob", new BigDecimal("50"));

        assertEquals(new BigDecimal("150"), account.getBalance());
        assertEquals(new BigDecimal("50"), recipient.getBalance());
    }

    @Test
    void transferAmount_shouldThrowWhenInsufficientFunds() {
        account.setBalance(new BigDecimal("10"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(account, "bob", new BigDecimal("50")));

        assertEquals("Insufficient funds", ex.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmount_shouldThrowWhenRecipientNotFound() {
        account.setBalance(new BigDecimal("200"));

        when(accountRepository.findByUsername("bob")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(account, "bob", new BigDecimal("50")));

        assertEquals("Recipient account not found", ex.getMessage());
    }

    @Test
    void transferAmount_shouldCreateTwoTransactionRecords() {
        account.setBalance(new BigDecimal("200"));

        Account recipient = new Account();
        recipient.setId(2L);
        recipient.setUsername("bob");
        recipient.setBalance(new BigDecimal("0"));

        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(account, "bob", new BigDecimal("50"));

        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    // ----- registerAccount() -----

    @Test
    void registerAccount_shouldCreateAccountWithZeroBalance() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.registerAccount("newuser", "password");

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());

        Account saved = captor.getValue();
        assertEquals("newuser", saved.getUsername());
        assertEquals("encoded", saved.getPassword());
        assertEquals(BigDecimal.ZERO, saved.getBalance());
    }

    @Test
    void registerAccount_shouldThrowWhenUsernameAlreadyExists() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(account));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("alice", "password"));

        assertEquals("Username already exists", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    // ----- findAccountByUsername() -----

    @Test
    void findAccountByUsername_shouldReturnAccountWhenFound() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(account));

        Account result = accountService.findAccountByUsername("alice");

        assertSame(account, result);
    }

    @Test
    void findAccountByUsername_shouldThrowWhenNotFound() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("ghost"));

        assertEquals("Account not found", ex.getMessage());
    }

    // ----- loadUserByUsername() -----

    @Test
    void loadUserByUsername_shouldReturnUserDetailsWhenFound() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(account));

        UserDetails userDetails = accountService.loadUserByUsername("alice");

        assertEquals("alice", userDetails.getUsername());
        assertEquals("secret", userDetails.getPassword());
    }

    @Test
    void loadUserByUsername_shouldThrowWhenUsernameNotFound() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("ghost"));
    }

    // ----- getTransactionHistory() -----

    @Test
    void getTransactionHistory_shouldReturnTransactionsForAccount() {
        Transaction t1 = new Transaction(new BigDecimal("50"), "Deposit", LocalDateTime.now(), account);
        Transaction t2 = new Transaction(new BigDecimal("30"), "Withdrawal", LocalDateTime.now(), account);
        List<Transaction> transactions = List.of(t1, t2);

        when(transactionRepository.findByAccountId(1L)).thenReturn(transactions);

        List<Transaction> result = accountService.getTransactionHistory(account);

        assertEquals(2, result.size());
        assertEquals(transactions, result);
    }
}
