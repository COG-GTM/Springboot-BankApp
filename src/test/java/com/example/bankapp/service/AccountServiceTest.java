package com.example.bankapp.service;

import com.example.bankapp.event.TransactionEvent;
import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collections;
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUsername("testuser");
        testAccount.setPassword("encodedPassword");
        testAccount.setBalance(new BigDecimal("1000"));
        testAccount.setTransactions(Collections.emptyList());
    }

    @Test
    void registerAccount_success() {
        when(accountRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.registerAccount("newuser", "password");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void registerAccount_duplicateUsername() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("testuser", "password"));
        assertEquals("Username already exists", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void deposit_success() {
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        accountService.deposit(testAccount, new BigDecimal("500"));

        assertEquals(new BigDecimal("1500"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(eventPublisher).publishEvent(any(TransactionEvent.class));
    }

    @Test
    void deposit_correctAmount() {
        testAccount.setBalance(new BigDecimal("500"));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        accountService.deposit(testAccount, new BigDecimal("100"));

        assertEquals(new BigDecimal("600"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);

        ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        TransactionEvent event = eventCaptor.getValue();
        assertEquals(new BigDecimal("100"), event.getAmount());
        assertEquals("Deposit", event.getType());
        assertEquals(1L, event.getAccountId());
    }

    @Test
    void withdraw_success() {
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        accountService.withdraw(testAccount, new BigDecimal("500"));

        assertEquals(new BigDecimal("500"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(eventPublisher).publishEvent(any(TransactionEvent.class));
    }

    @Test
    void withdraw_insufficientFunds() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(testAccount, new BigDecimal("1500")));
        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void transferAmount_success() {
        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUsername("recipient");
        toAccount.setPassword("encodedPassword");
        toAccount.setBalance(new BigDecimal("500"));

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.transferAmount(testAccount, "recipient", new BigDecimal("300"));

        assertEquals(new BigDecimal("700"), testAccount.getBalance());
        assertEquals(new BigDecimal("800"), toAccount.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(eventPublisher, times(2)).publishEvent(any(TransactionEvent.class));
    }

    @Test
    void transferAmount_insufficientFunds() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(testAccount, "recipient", new BigDecimal("1500")));
        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transferAmount_recipientNotFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(testAccount, "unknown", new BigDecimal("100")));
        assertEquals("Recipient account not found", exception.getMessage());
    }

    @Test
    void loadUserByUsername_success() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(testAccount));

        UserDetails userDetails = accountService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
    }

    @Test
    void loadUserByUsername_notFound() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> accountService.loadUserByUsername("unknown"));
    }
}
