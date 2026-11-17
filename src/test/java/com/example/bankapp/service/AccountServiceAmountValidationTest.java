package com.example.bankapp.service;

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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceAmountValidationTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account attacker;
    private Account victim;

    @BeforeEach
    void setUp() {
        attacker = new Account();
        attacker.setUsername("attacker");
        attacker.setBalance(new BigDecimal("100.00"));

        victim = new Account();
        victim.setUsername("victim");
        victim.setBalance(new BigDecimal("5000.00"));

        when(accountRepository.findByUsername("victim")).thenReturn(Optional.of(victim));
        when(accountRepository.findByUsername("attacker")).thenReturn(Optional.of(attacker));
    }

    @Test
    void transferRejectsNegativeAmountWithoutMovingMoney() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(attacker, "victim", new BigDecimal("-1000000")));

        assertEquals("Amount must be greater than zero", e.getMessage());
        assertEquals(new BigDecimal("100.00"), attacker.getBalance());
        assertEquals(new BigDecimal("5000.00"), victim.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferRejectsZeroAndNullAmount() {
        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(attacker, "victim", BigDecimal.ZERO));
        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(attacker, "victim", null));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transferRejectsSelfTransfer() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(attacker, "attacker", new BigDecimal("10.00")));

        assertEquals("Cannot transfer to the same account", e.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transferMovesMoneyForPositiveAmount() {
        accountService.transferAmount(attacker, "victim", new BigDecimal("40.00"));

        assertEquals(new BigDecimal("60.00"), attacker.getBalance());
        assertEquals(new BigDecimal("5040.00"), victim.getBalance());
    }

    @Test
    void depositRejectsNonPositiveAmount() {
        assertThrows(RuntimeException.class, () -> accountService.deposit(attacker, new BigDecimal("-50.00")));
        assertThrows(RuntimeException.class, () -> accountService.deposit(attacker, BigDecimal.ZERO));
        assertEquals(new BigDecimal("100.00"), attacker.getBalance());
    }

    @Test
    void withdrawRejectsNonPositiveAmount() {
        assertThrows(RuntimeException.class, () -> accountService.withdraw(attacker, new BigDecimal("-50.00")));
        assertEquals(new BigDecimal("100.00"), attacker.getBalance());
    }

    @Test
    void amountAboveMaximumOrWithTooManyDecimalsIsRejected() {
        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(attacker, "victim", new BigDecimal("1000000.01")));
        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(attacker, "victim", new BigDecimal("10.001")));
        verify(accountRepository, never()).save(any(Account.class));
    }
}
