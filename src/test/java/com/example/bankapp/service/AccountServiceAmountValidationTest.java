package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceAmountValidationTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AccountService accountService;

    @Test
    void transferAmountRejectsNegativeAmountBeforeRepositoryInteractions() {
        Account sender = account("sender", "50");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> accountService.transferAmount(sender, "victim", new BigDecimal("-100"))
        );

        assertEquals("Amount must be greater than zero", exception.getMessage());
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void transferAmountRejectsZeroAmount() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> accountService.transferAmount(account("sender", "50"), "victim", BigDecimal.ZERO)
        );

        assertEquals("Amount must be greater than zero", exception.getMessage());
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void transferAmountRejectsNullAmount() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> accountService.transferAmount(account("sender", "50"), "victim", null)
        );

        assertEquals("Amount must be greater than zero", exception.getMessage());
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void withdrawRejectsNegativeAmountBeforeRepositoryInteractions() {
        Account account = account("account", "100");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("-100"))
        );

        assertEquals("Amount must be greater than zero", exception.getMessage());
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void depositRejectsNegativeAmountBeforeRepositoryInteractions() {
        Account account = account("account", "100");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> accountService.deposit(account, new BigDecimal("-100"))
        );

        assertEquals("Amount must be greater than zero", exception.getMessage());
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void transferAmountUpdatesBothAccountsAndSavesTransactions() {
        Account sender = account("sender", "100");
        Account recipient = account("victim", "0");
        when(accountRepository.findByUsername("victim")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "victim", new BigDecimal("25"));

        assertEquals(new BigDecimal("75"), sender.getBalance());
        assertEquals(new BigDecimal("25"), recipient.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    void withdrawSubtractsAmountFromBalance() {
        Account account = account("account", "100");

        accountService.withdraw(account, new BigDecimal("25"));

        assertEquals(new BigDecimal("75"), account.getBalance());
    }

    @Test
    void depositAddsAmountToBalance() {
        Account account = account("account", "100");

        accountService.deposit(account, new BigDecimal("25"));

        assertEquals(new BigDecimal("125"), account.getBalance());
    }

    private static Account account(String username, String balance) {
        Account account = new Account();
        account.setUsername(username);
        account.setBalance(new BigDecimal(balance));
        return account;
    }
}
