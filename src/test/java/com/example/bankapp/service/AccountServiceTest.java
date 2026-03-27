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
    void findAccountByUsername_notFound_throwsException() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.findAccountByUsername("unknown"));
        assertEquals("Account not found", ex.getMessage());
    }

    @Test
    void registerAccount_happyPath() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.registerAccount("newuser", "password");

        assertEquals("newuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void registerAccount_usernameExists_throwsException() {
        when(accountRepository.findByUsername("existing")).thenReturn(Optional.of(new Account()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("existing", "password"));
        assertEquals("Username already exists", ex.getMessage());
    }

    @Test
    void deposit_updatesBalanceAndSavesTransaction() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100.00"));

        accountService.deposit(account, new BigDecimal("50.00"));

        assertEquals(new BigDecimal("150.00"), account.getBalance());
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertEquals(new BigDecimal("50.00"), savedTx.getAmount());
        assertEquals("Deposit", savedTx.getType());
        assertEquals(account, savedTx.getAccount());
    }

    @Test
    void withdraw_happyPath_subtractsBalanceAndSavesTransaction() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100.00"));

        accountService.withdraw(account, new BigDecimal("30.00"));

        assertEquals(new BigDecimal("70.00"), account.getBalance());
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertEquals(new BigDecimal("30.00"), savedTx.getAmount());
        assertEquals("Withdrawal", savedTx.getType());
        assertEquals(account, savedTx.getAccount());
    }

    @Test
    void withdraw_insufficientFunds_throwsException() {
        Account account = new Account();
        account.setBalance(new BigDecimal("10.00"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("50.00")));
        assertEquals("Insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void transferAmount_happyPath() {
        Account sender = new Account();
        sender.setUsername("sender");
        sender.setBalance(new BigDecimal("200.00"));

        Account recipient = new Account();
        recipient.setUsername("recipient");
        recipient.setBalance(new BigDecimal("50.00"));

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "recipient", new BigDecimal("75.00"));

        assertEquals(new BigDecimal("125.00"), sender.getBalance());
        assertEquals(new BigDecimal("125.00"), recipient.getBalance());

        verify(accountRepository).save(sender);
        verify(accountRepository).save(recipient);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());
        List<Transaction> savedTxs = txCaptor.getAllValues();

        assertEquals("Transfer Out to recipient", savedTxs.get(0).getType());
        assertEquals(sender, savedTxs.get(0).getAccount());
        assertEquals("Transfer In from sender", savedTxs.get(1).getType());
        assertEquals(recipient, savedTxs.get(1).getAccount());
    }

    @Test
    void transferAmount_insufficientFunds_throwsException() {
        Account sender = new Account();
        sender.setBalance(new BigDecimal("10.00"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "recipient", new BigDecimal("50.00")));
        assertEquals("Insufficient funds", ex.getMessage());
    }

    @Test
    void transferAmount_recipientNotFound_throwsException() {
        Account sender = new Account();
        sender.setBalance(new BigDecimal("200.00"));

        when(accountRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "nobody", new BigDecimal("50.00")));
        assertEquals("Recipient account not found", ex.getMessage());
    }

    @Test
    void getTransactionHistory_delegatesToRepository() {
        Account account = new Account();
        account.setId(1L);
        List<Transaction> transactions = Collections.singletonList(new Transaction());
        when(transactionRepository.findByAccountId(1L)).thenReturn(transactions);

        List<Transaction> result = accountService.getTransactionHistory(account);

        assertEquals(transactions, result);
        verify(transactionRepository).findByAccountId(1L);
    }

    @Test
    void loadUserByUsername_happyPath() {
        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword("encodedPass");
        account.setBalance(new BigDecimal("500.00"));
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        UserDetails userDetails = accountService.loadUserByUsername("testuser");

        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPass", userDetails.getPassword());
    }

    @Test
    void loadUserByUsername_notFound_throwsRuntimeException() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("unknown"));
    }

    @Test
    void authorities_returnsUserAuthority() {
        var authorities = accountService.authorities();

        assertEquals(1, authorities.size());
        GrantedAuthority authority = authorities.iterator().next();
        assertEquals("USER", authority.getAuthority());
    }
}
