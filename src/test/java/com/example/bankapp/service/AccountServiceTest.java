package com.example.bankapp.service;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.DuplicateUsernameException;
import com.example.bankapp.exception.InsufficientFundsException;
import com.example.bankapp.exception.InvalidAmountException;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        account.setPassword("encoded");
        account.setBalance(new BigDecimal("1000.00"));
    }

    @Test
    void deposit_increasesBalanceAndSavesTransaction() {
        BigDecimal amount = new BigDecimal("500.00");

        accountService.deposit(account, amount);

        assertEquals(new BigDecimal("1500.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_decreasesBalanceAndSavesTransaction() {
        BigDecimal amount = new BigDecimal("300.00");

        accountService.withdraw(account, amount);

        assertEquals(new BigDecimal("700.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_insufficientFunds_throwsException() {
        BigDecimal amount = new BigDecimal("2000.00");

        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class,
                () -> accountService.withdraw(account, amount));
        assertEquals("Insufficient funds", ex.getMessage());
    }

    @Test
    void withdraw_exactBalance_succeeds() {
        BigDecimal amount = new BigDecimal("1000.00");

        accountService.withdraw(account, amount);

        assertEquals(BigDecimal.ZERO.setScale(2), account.getBalance().setScale(2));
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void transferAmount_happyPath() {
        Account recipient = new Account();
        recipient.setId(2L);
        recipient.setUsername("recipient");
        recipient.setBalance(new BigDecimal("500.00"));

        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(account, "recipient", new BigDecimal("200.00"));

        assertEquals(new BigDecimal("800.00"), account.getBalance());
        assertEquals(new BigDecimal("700.00"), recipient.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transferAmount_insufficientFunds_throwsException() {
        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class,
                () -> accountService.transferAmount(account, "recipient", new BigDecimal("5000.00")));
        assertEquals("Insufficient funds", ex.getMessage());
    }

    @Test
    void transferAmount_recipientNotFound_throwsException() {
        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> accountService.transferAmount(account, "nonexistent", new BigDecimal("100.00")));
        assertEquals("Recipient account not found", ex.getMessage());
    }

    @Test
    void findAccountByUsername_notFound_throwsException() {
        when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> accountService.findAccountByUsername("unknown"));
        assertEquals("Account not found", ex.getMessage());
    }

    @Test
    void registerAccount_duplicateUsername_throwsException() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(account));

        DuplicateUsernameException ex = assertThrows(DuplicateUsernameException.class,
                () -> accountService.registerAccount("testuser", "password"));
        assertEquals("Username already exists", ex.getMessage());
    }

    @Test
    void deposit_zeroAmount_throwsInvalidAmountException() {
        assertThrows(InvalidAmountException.class,
                () -> accountService.deposit(account, BigDecimal.ZERO));
    }

    @Test
    void deposit_negativeAmount_throwsInvalidAmountException() {
        assertThrows(InvalidAmountException.class,
                () -> accountService.deposit(account, new BigDecimal("-100.00")));
    }

    @Test
    void withdraw_negativeAmount_throwsInvalidAmountException() {
        assertThrows(InvalidAmountException.class,
                () -> accountService.withdraw(account, new BigDecimal("-50.00")));
    }

    @Test
    void transfer_negativeAmount_throwsInvalidAmountException() {
        assertThrows(InvalidAmountException.class,
                () -> accountService.transferAmount(account, "someone", new BigDecimal("-200.00")));
    }
}
