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
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account account(BigDecimal balance) {
        Account a = new Account();
        a.setId(1L);
        a.setUsername("alice");
        a.setBalance(balance);
        return a;
    }

    // ---------- Input validation: reject non-positive amounts server-side ----------

    @Test
    void deposit_rejectsZeroAmount() {
        Account acc = account(new BigDecimal("100"));
        assertThrows(IllegalArgumentException.class,
                () -> accountService.deposit(acc, BigDecimal.ZERO));
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        assertEquals(new BigDecimal("100"), acc.getBalance());
    }

    @Test
    void deposit_rejectsNegativeAmount() {
        Account acc = account(new BigDecimal("100"));
        assertThrows(IllegalArgumentException.class,
                () -> accountService.deposit(acc, new BigDecimal("-50")));
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_rejectsZeroAmount() {
        Account acc = account(new BigDecimal("100"));
        assertThrows(IllegalArgumentException.class,
                () -> accountService.withdraw(acc, BigDecimal.ZERO));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_rejectsNegativeAmount() {
        Account acc = account(new BigDecimal("100"));
        assertThrows(IllegalArgumentException.class,
                () -> accountService.withdraw(acc, new BigDecimal("-1")));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void transfer_rejectsZeroAmount() {
        Account from = account(new BigDecimal("100"));
        assertThrows(IllegalArgumentException.class,
                () -> accountService.transferAmount(from, "bob", BigDecimal.ZERO));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void transfer_rejectsNegativeAmount() {
        Account from = account(new BigDecimal("100"));
        // A negative amount previously let a sender *steal* from the recipient.
        assertThrows(IllegalArgumentException.class,
                () -> accountService.transferAmount(from, "bob", new BigDecimal("-25")));
        verify(accountRepository, never()).save(any());
        verify(accountRepository, never()).findByUsername("bob");
    }

    // ---------- No behavior change for valid inputs ----------

    @Test
    void deposit_validAmountUpdatesBalanceAndRecordsTransaction() {
        Account acc = account(new BigDecimal("100"));
        accountService.deposit(acc, new BigDecimal("40"));
        assertEquals(new BigDecimal("140"), acc.getBalance());
        verify(accountRepository).save(acc);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_validAmountUpdatesBalance() {
        Account acc = account(new BigDecimal("100"));
        accountService.withdraw(acc, new BigDecimal("30"));
        assertEquals(new BigDecimal("70"), acc.getBalance());
        verify(accountRepository).save(acc);
    }

    @Test
    void transfer_validAmountMovesFundsBetweenAccounts() {
        Account from = account(new BigDecimal("100"));
        Account to = new Account();
        to.setId(2L);
        to.setUsername("bob");
        to.setBalance(new BigDecimal("10"));
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(to));

        accountService.transferAmount(from, "bob", new BigDecimal("25"));

        assertEquals(new BigDecimal("75"), from.getBalance());
        assertEquals(new BigDecimal("35"), to.getBalance());
        verify(accountRepository).save(from);
        verify(accountRepository).save(to);
    }

    // ---------- Transaction atomicity ----------

    @Test
    void transferAmount_isAnnotatedTransactional() throws NoSuchMethodException {
        Method m = AccountService.class.getMethod(
                "transferAmount", Account.class, String.class, BigDecimal.class);
        Transactional tx = m.getAnnotation(Transactional.class);
        assertNotNull(tx, "transferAmount must be @Transactional so a mid-operation "
                + "failure rolls back and cannot lose funds");
    }

    @Test
    void deposit_isAnnotatedTransactional() throws NoSuchMethodException {
        Method m = AccountService.class.getMethod("deposit", Account.class, BigDecimal.class);
        assertNotNull(m.getAnnotation(Transactional.class),
                "deposit must be @Transactional so the balance and ledger writes are atomic");
    }

    @Test
    void withdraw_isAnnotatedTransactional() throws NoSuchMethodException {
        Method m = AccountService.class.getMethod("withdraw", Account.class, BigDecimal.class);
        assertNotNull(m.getAnnotation(Transactional.class),
                "withdraw must be @Transactional so the balance and ledger writes are atomic");
    }

    @Test
    void transfer_propagatesFailureSoTransactionRollsBack() {
        Account from = account(new BigDecimal("100"));
        // Recipient lookup fails mid-operation: the exception must propagate out of the
        // @Transactional method so Spring rolls back the sender debit (no lost funds).
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(from, "ghost", new BigDecimal("25")));
    }

    @Test
    void withdraw_stillRejectsInsufficientFunds() {
        Account acc = account(new BigDecimal("10"));
        assertThrows(RuntimeException.class,
                () -> accountService.withdraw(acc, new BigDecimal("50")));
    }

    @Test
    void validPositiveTransferDoesNotThrow() {
        Account from = account(new BigDecimal("100"));
        Account to = new Account();
        to.setBalance(new BigDecimal("0"));
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(to));
        assertDoesNotThrow(() -> accountService.transferAmount(from, "bob", new BigDecimal("1")));
    }
}
