package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
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

    @Test
    void findAccountByUsername_happyPath() {
        Account account = new Account();
        account.setUsername("testuser");
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        Account result = accountService.findAccountByUsername("testuser");

        assertEquals("testuser", result.getUsername());
        verify(accountRepository).findByUsername("testuser");
    }

    @Test
    void findAccountByUsername_notFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("unknown"));
        assertEquals("Account not found", exception.getMessage());
    }

    @Test
    void registerAccount_happyPath() {
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
    void registerAccount_usernameAlreadyExists() {
        when(accountRepository.findByUsername("existing")).thenReturn(Optional.of(new Account()));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("existing", "password"));
        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void deposit_increasesBalanceAndSavesTransaction() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100.00"));

        accountService.deposit(account, new BigDecimal("50.00"));

        assertEquals(new BigDecimal("150.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(argThat(transaction ->
                transaction.getAmount().equals(new BigDecimal("50.00"))
                        && transaction.getType().equals("Deposit")
                        && transaction.getAccount() == account));
    }

    @Test
    void withdraw_sufficientFunds() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100.00"));

        accountService.withdraw(account, new BigDecimal("30.00"));

        assertEquals(new BigDecimal("70.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(argThat(transaction ->
                transaction.getAmount().equals(new BigDecimal("30.00"))
                        && transaction.getType().equals("Withdrawal")
                        && transaction.getAccount() == account));
    }

    @Test
    void withdraw_insufficientFunds() {
        Account account = new Account();
        account.setBalance(new BigDecimal("10.00"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("50.00")));
        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void transferAmount_happyPath() {
        Account fromAccount = new Account();
        fromAccount.setUsername("sender");
        fromAccount.setBalance(new BigDecimal("200.00"));

        Account toAccount = new Account();
        toAccount.setUsername("recipient");
        toAccount.setBalance(new BigDecimal("50.00"));

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(toAccount));

        accountService.transferAmount(fromAccount, "recipient", new BigDecimal("75.00"));

        assertEquals(new BigDecimal("125.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("125.00"), toAccount.getBalance());
        verify(accountRepository).save(fromAccount);
        verify(accountRepository).save(toAccount);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transferAmount_insufficientFunds() {
        Account fromAccount = new Account();
        fromAccount.setBalance(new BigDecimal("10.00"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(fromAccount, "recipient", new BigDecimal("50.00")));
        assertEquals("Insufficient funds", exception.getMessage());
    }

    @Test
    void transferAmount_recipientNotFound() {
        Account fromAccount = new Account();
        fromAccount.setBalance(new BigDecimal("200.00"));

        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(fromAccount, "unknown", new BigDecimal("50.00")));
        assertEquals("Recipient account not found", exception.getMessage());
    }

    @Test
    void getTransactionHistory_delegatesToRepository() {
        Account account = new Account();
        account.setId(1L);
        List<Transaction> expectedTransactions = List.of(new Transaction());
        when(transactionRepository.findByAccountId(1L)).thenReturn(expectedTransactions);

        List<Transaction> result = accountService.getTransactionHistory(account);

        assertEquals(expectedTransactions, result);
        verify(transactionRepository).findByAccountId(1L);
    }

    @Test
    void loadUserByUsername_returnsUserDetails() {
        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword("encodedPass");
        account.setBalance(new BigDecimal("100.00"));
        account.setTransactions(Collections.emptyList());
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        UserDetails userDetails = accountService.loadUserByUsername("testuser");

        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPass", userDetails.getPassword());
        assertFalse(userDetails.getAuthorities().isEmpty());
    }

    @Test
    void loadUserByUsername_userNotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("unknown"));
    }

    @Test
    void authorities_returnsUserAuthority() {
        assertFalse(accountService.authorities().isEmpty());
        assertEquals("USER", accountService.authorities().iterator().next().getAuthority());
    }
}
