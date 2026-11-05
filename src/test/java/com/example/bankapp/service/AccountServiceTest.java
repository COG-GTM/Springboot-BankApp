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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @Test
    void withdrawRejectsNegativeAmountWithoutChangingBalance() {
        Account account = accountWithBalance("100");

        assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("-1000000")));

        assertEquals(new BigDecimal("100"), account.getBalance());
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void withdrawRejectsZeroAmount() {
        Account account = accountWithBalance("100");

        assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, BigDecimal.ZERO));

        assertEquals(new BigDecimal("100"), account.getBalance());
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void withdrawRejectsNullAmount() {
        Account account = accountWithBalance("100");

        assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, null));

        assertEquals(new BigDecimal("100"), account.getBalance());
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void withdrawUpdatesBalanceAndSavesTransaction() {
        Account account = accountWithBalance("100");

        accountService.withdraw(account, new BigDecimal("40"));

        assertEquals(new BigDecimal("60"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void depositRejectsNegativeAmountWithoutChangingBalance() {
        Account account = accountWithBalance("100");

        assertThrows(RuntimeException.class,
                () -> accountService.deposit(account, new BigDecimal("-40")));

        assertEquals(new BigDecimal("100"), account.getBalance());
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void depositUpdatesBalanceAndSavesTransaction() {
        Account account = accountWithBalance("100");

        accountService.deposit(account, new BigDecimal("40"));

        assertEquals(new BigDecimal("140"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void transferRejectsNegativeAmountBeforeLookingUpRecipient() {
        Account account = accountWithBalance("100");

        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(account, "recipient", new BigDecimal("-40")));

        verifyNoInteractions(accountRepository, transactionRepository);
    }

    private Account accountWithBalance(String balance) {
        Account account = new Account();
        account.setUsername("account");
        account.setBalance(new BigDecimal(balance));
        return account;
    }
}
