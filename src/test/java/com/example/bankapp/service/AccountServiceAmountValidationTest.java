package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AccountServiceAmountValidationTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setUsername("attacker");
        account.setBalance(new BigDecimal("100.00"));
    }

    @Test
    void withdrawRejectsNegativeAmount() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("-1000000")));

        assertEquals("Amount must be greater than zero", exception.getMessage());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void depositRejectsNegativeAmount() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.deposit(account, new BigDecimal("-50")));

        assertEquals("Amount must be greater than zero", exception.getMessage());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void depositRejectsZeroAndNullAmount() {
        assertThrows(RuntimeException.class, () -> accountService.deposit(account, BigDecimal.ZERO));
        assertThrows(RuntimeException.class, () -> accountService.deposit(account, null));
        assertThrows(RuntimeException.class, () -> accountService.withdraw(account, BigDecimal.ZERO));
        assertThrows(RuntimeException.class, () -> accountService.withdraw(account, null));
        assertEquals(new BigDecimal("100.00"), account.getBalance());
    }

    @Test
    void depositRejectsAmountAboveMaximum() {
        assertThrows(RuntimeException.class, () -> accountService.deposit(account, new BigDecimal("1000000.01")));
        assertEquals(new BigDecimal("100.00"), account.getBalance());
    }

    @Test
    void depositRejectsSubCentPrecision() {
        assertThrows(RuntimeException.class, () -> accountService.deposit(account, new BigDecimal("1.001")));
        assertEquals(new BigDecimal("100.00"), account.getBalance());
    }

    @Test
    void depositAndWithdrawAcceptPositiveAmounts() {
        accountService.deposit(account, new BigDecimal("25.50"));
        assertEquals(new BigDecimal("125.50"), account.getBalance());

        accountService.withdraw(account, new BigDecimal("25.50"));
        assertEquals(new BigDecimal("100.00"), account.getBalance());
    }

    @Test
    void withdrawStillRejectsAmountAboveBalance() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("100.01")));

        assertEquals("Insufficient funds", exception.getMessage());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
    }
}
