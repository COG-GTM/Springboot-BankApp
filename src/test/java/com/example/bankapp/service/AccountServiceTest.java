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

    // --- findAccountByUsername ---

    @Test
    void findAccountByUsername_found() {
        Account account = new Account();
        account.setUsername("user1");
        when(accountRepository.findByUsername("user1")).thenReturn(Optional.of(account));

        Account result = accountService.findAccountByUsername("user1");
        assertEquals("user1", result.getUsername());
    }

    @Test
    void findAccountByUsername_notFound() {
        when(accountRepository.findByUsername("missing")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("missing"));
        assertEquals("Account not found", ex.getMessage());
    }

    // --- registerAccount ---

    @Test
    void registerAccount_success() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.registerAccount("newuser", "pass");
        assertEquals("newuser", result.getUsername());
        assertEquals("encoded", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void registerAccount_usernameAlreadyExists() {
        when(accountRepository.findByUsername("existing")).thenReturn(Optional.of(new Account()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("existing", "pass"));
        assertEquals("Username already exists", ex.getMessage());
        verify(accountRepository, never()).save(any());
    }

    // --- deposit ---

    @Test
    void deposit_success() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100"));

        accountService.deposit(account, new BigDecimal("50"));

        assertEquals(new BigDecimal("150"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    // --- withdraw ---

    @Test
    void withdraw_success() {
        Account account = new Account();
        account.setBalance(new BigDecimal("200"));

        accountService.withdraw(account, new BigDecimal("50"));

        assertEquals(new BigDecimal("150"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_insufficientFunds() {
        Account account = new Account();
        account.setBalance(new BigDecimal("10"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("50")));
        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    // --- getTransactionHistory ---

    @Test
    void getTransactionHistory_success() {
        Account account = new Account();
        account.setId(1L);
        List<Transaction> transactions = Collections.singletonList(new Transaction());
        when(transactionRepository.findByAccountId(1L)).thenReturn(transactions);

        List<Transaction> result = accountService.getTransactionHistory(account);
        assertEquals(1, result.size());
    }

    // --- loadUserByUsername ---

    @Test
    void loadUserByUsername_success() {
        Account account = new Account();
        account.setUsername("user1");
        account.setPassword("encoded");
        account.setBalance(BigDecimal.TEN);
        account.setTransactions(Collections.emptyList());
        when(accountRepository.findByUsername("user1")).thenReturn(Optional.of(account));

        UserDetails result = accountService.loadUserByUsername("user1");
        assertEquals("user1", result.getUsername());
        assertEquals("encoded", result.getPassword());
        assertNotNull(result.getAuthorities());
    }

    @Test
    void loadUserByUsername_notFound() {
        when(accountRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("missing"));
    }

    @Test
    void loadUserByUsername_accountNull() {
        AccountService spyService = spy(accountService);
        doReturn(null).when(spyService).findAccountByUsername("nulluser");

        assertThrows(UsernameNotFoundException.class,
                () -> spyService.loadUserByUsername("nulluser"));
    }

    // --- authorities ---

    @Test
    void authorities_returnsUserAuthority() {
        Collection<? extends GrantedAuthority> auths = accountService.authorities();
        assertEquals(1, auths.size());
        assertEquals("USER", auths.iterator().next().getAuthority());
    }

    // --- transferAmount ---

    @Test
    void transferAmount_success() {
        Account fromAccount = new Account();
        fromAccount.setUsername("sender");
        fromAccount.setBalance(new BigDecimal("500"));

        Account toAccount = new Account();
        toAccount.setUsername("receiver");
        toAccount.setBalance(new BigDecimal("100"));

        when(accountRepository.findByUsername("receiver")).thenReturn(Optional.of(toAccount));

        accountService.transferAmount(fromAccount, "receiver", new BigDecimal("200"));

        assertEquals(new BigDecimal("300"), fromAccount.getBalance());
        assertEquals(new BigDecimal("300"), toAccount.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transferAmount_insufficientFunds() {
        Account fromAccount = new Account();
        fromAccount.setBalance(new BigDecimal("10"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(fromAccount, "receiver", new BigDecimal("100")));
        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void transferAmount_recipientNotFound() {
        Account fromAccount = new Account();
        fromAccount.setBalance(new BigDecimal("500"));

        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(fromAccount, "unknown", new BigDecimal("100")));
        assertEquals("Recipient account not found", ex.getMessage());
    }
}
