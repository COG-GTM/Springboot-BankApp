package com.example.bankapp.service;

import com.example.bankapp.audit.AuditLogger;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Control APP-TXN-01: monetary amounts must be positive and within the per-transaction limit
 * before any balance is mutated or transaction record written.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceValidationTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AccountService accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setUsername("alice");
        account.setBalance(new BigDecimal("100.00"));
    }

    private Account recipient() {
        Account bob = new Account();
        bob.setId(2L);
        bob.setUsername("bob");
        bob.setBalance(new BigDecimal("50.00"));
        return bob;
    }

    private void assertNothingPersisted() {
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void depositRejectsNegativeAmount() {
        assertThatThrownBy(() -> accountService.deposit(account, new BigDecimal("-50.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        assertNothingPersisted();
    }

    @Test
    void depositRejectsZeroAmount() {
        assertThatThrownBy(() -> accountService.deposit(account, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertNothingPersisted();
    }

    @Test
    void depositRejectsAmountAboveTransactionLimit() {
        BigDecimal overLimit = AccountService.MAX_TRANSACTION_AMOUNT.add(new BigDecimal("0.01"));

        assertThatThrownBy(() -> accountService.deposit(account, overLimit))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("per-transaction limit");
        assertNothingPersisted();
    }

    @Test
    void withdrawRejectsNegativeAmount() {
        assertThatThrownBy(() -> accountService.withdraw(account, new BigDecimal("-10.00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertNothingPersisted();
    }

    @Test
    void withdrawRejectsOverdraft() {
        assertThatThrownBy(() -> accountService.withdraw(account, new BigDecimal("100.01")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        assertNothingPersisted();
    }

    @Test
    void transferRejectsNegativeAmountWithoutTouchingRecipient() {
        lenient().when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient()));

        assertThatThrownBy(() -> accountService.transferAmount(account, "bob", new BigDecimal("-500.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        assertNothingPersisted();
    }

    @Test
    void transferRejectsOverdraft() {
        lenient().when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient()));

        assertThatThrownBy(() -> accountService.transferAmount(account, "bob", new BigDecimal("500.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");
        assertNothingPersisted();
    }

    @Test
    void transferRejectsSelfTransfer() {
        assertThatThrownBy(() -> accountService.transferAmount(account, "alice", new BigDecimal("10.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same account");
        assertNothingPersisted();
    }

    @Test
    void failedTransactionsAreAuditLogged() {
        assertThatThrownBy(() -> accountService.deposit(account, new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(auditLogger).failure(eq("alice"), eq("DEPOSIT"), any(BigDecimal.class), eq(1L), isNull(), anyString());
    }
}
