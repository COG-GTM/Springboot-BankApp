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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
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

    // --- findAccountByUsername ---

    @Test
    void findAccountByUsername_success() {
        Account account = new Account();
        account.setUsername("john");
        when(accountRepository.findByUsername("john")).thenReturn(Optional.of(account));

        Account result = accountService.findAccountByUsername("john");

        assertEquals("john", result.getUsername());
        verify(accountRepository).findByUsername("john");
    }

    @Test
    void findAccountByUsername_notFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("unknown"));
        assertEquals("Account not found", ex.getMessage());
    }

    // --- registerAccount ---

    @Test
    void registerAccount_success() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPass");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.registerAccount("newuser", "pass123");

        assertEquals("newuser", result.getUsername());
        assertEquals("encodedPass", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void registerAccount_duplicateUsername() {
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
        account.setBalance(new BigDecimal("100.00"));

        accountService.deposit(account, new BigDecimal("50.00"));

        assertEquals(new BigDecimal("150.00"), account.getBalance());
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertEquals("Deposit", txCaptor.getValue().getType());
        assertEquals(new BigDecimal("50.00"), txCaptor.getValue().getAmount());
    }

    @Test
    void deposit_zeroInitialBalance() {
        Account account = new Account();
        account.setBalance(BigDecimal.ZERO);

        accountService.deposit(account, new BigDecimal("200.00"));

        assertEquals(new BigDecimal("200.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    // --- withdraw ---

    @Test
    void withdraw_success() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100.00"));

        accountService.withdraw(account, new BigDecimal("50.00"));

        assertEquals(new BigDecimal("50.00"), account.getBalance());
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertEquals("Withdrawal", txCaptor.getValue().getType());
        assertEquals(new BigDecimal("50.00"), txCaptor.getValue().getAmount());
    }

    @Test
    void withdraw_insufficientFunds() {
        Account account = new Account();
        account.setBalance(new BigDecimal("30.00"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("50.00")));
        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    // --- getTransactionHistory ---

    @Test
    void getTransactionHistory_success() {
        Account account = new Account();
        account.setId(1L);
        List<Transaction> transactions = List.of(new Transaction(), new Transaction());
        when(transactionRepository.findByAccountId(1L)).thenReturn(transactions);

        List<Transaction> result = accountService.getTransactionHistory(account);

        assertEquals(2, result.size());
        verify(transactionRepository).findByAccountId(1L);
    }

    // --- loadUserByUsername ---

    @Test
    void loadUserByUsername_success() {
        Account account = new Account();
        account.setUsername("john");
        account.setPassword("encoded");
        account.setBalance(new BigDecimal("500.00"));
        account.setTransactions(new ArrayList<>());
        when(accountRepository.findByUsername("john")).thenReturn(Optional.of(account));

        UserDetails userDetails = accountService.loadUserByUsername("john");

        assertEquals("john", userDetails.getUsername());
        assertEquals("encoded", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("USER")));
    }

    @Test
    void loadUserByUsername_notFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("unknown"));
    }

    // --- authorities ---

    @Test
    void authorities_returnsUserAuthority() {
        Collection<? extends GrantedAuthority> auths = accountService.authorities();

        assertEquals(1, auths.size());
        assertTrue(auths.stream().anyMatch(a -> a.getAuthority().equals("USER")));
    }

    // --- transferAmount ---

    @Test
    void transferAmount_success() {
        Account sender = new Account();
        sender.setUsername("sender");
        sender.setBalance(new BigDecimal("100.00"));

        Account recipient = new Account();
        recipient.setUsername("recipient");
        recipient.setBalance(new BigDecimal("200.00"));

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "recipient", new BigDecimal("50.00"));

        assertEquals(new BigDecimal("50.00"), sender.getBalance());
        assertEquals(new BigDecimal("250.00"), recipient.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());
        List<Transaction> savedTransactions = txCaptor.getAllValues();
        assertEquals("Transfer Out to recipient", savedTransactions.get(0).getType());
        assertEquals("Transfer In from sender", savedTransactions.get(1).getType());
    }

    @Test
    void transferAmount_insufficientFunds() {
        Account sender = new Account();
        sender.setUsername("sender");
        sender.setBalance(new BigDecimal("30.00"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "recipient", new BigDecimal("50.00")));
        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferAmount_recipientNotFound() {
        Account sender = new Account();
        sender.setUsername("sender");
        sender.setBalance(new BigDecimal("100.00"));

        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "unknown", new BigDecimal("50.00")));
        assertEquals("Recipient account not found", ex.getMessage());
    }
}
