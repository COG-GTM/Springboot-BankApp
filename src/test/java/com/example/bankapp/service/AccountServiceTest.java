package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        fromAccount = account(1L, "alice", "1500.00");
        toAccount = account(2L, "bob", "250.00");
    }

    @Test
    void transferAmountSucceedsWhenUnderDailyLimit() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(toAccount));
        when(transactionRepository.sumOutboundTransferAmountByAccountIdAndTimestampBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("200.00"));

        accountService.transferAmount(fromAccount, "bob", new BigDecimal("300.00"));

        assertEquals(new BigDecimal("1200.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("550.00"), toAccount.getBalance());
        verify(accountRepository).save(fromAccount);
        verify(accountRepository).save(toAccount);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transferAmountSucceedsWhenExactlyAtDailyLimit() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(toAccount));
        when(transactionRepository.sumOutboundTransferAmountByAccountIdAndTimestampBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("700.00"));

        accountService.transferAmount(fromAccount, "bob", new BigDecimal("300.00"));

        assertEquals(new BigDecimal("1200.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("550.00"), toAccount.getBalance());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transferAmountFailsWhenDailyLimitWouldBeExceeded() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(toAccount));
        when(transactionRepository.sumOutboundTransferAmountByAccountIdAndTimestampBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("800.00"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(fromAccount, "bob", new BigDecimal("300.00")));

        assertEquals("Daily outbound transfer limit exceeded", exception.getMessage());
        assertEquals(new BigDecimal("1500.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("250.00"), toAccount.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmountFailsWhenFundsAreInsufficient() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(fromAccount, "bob", new BigDecimal("1600.00")));

        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository, never()).findByUsername("bob");
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmountFailsWhenAmountIsZero() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(fromAccount, "bob", BigDecimal.ZERO));

        assertEquals("Transfer amount must be greater than zero", exception.getMessage());
        verify(accountRepository, never()).findByUsername("bob");
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmountFailsWhenAmountIsNegative() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(fromAccount, "bob", new BigDecimal("-1.00")));

        assertEquals("Transfer amount must be greater than zero", exception.getMessage());
        verify(accountRepository, never()).findByUsername("bob");
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transferAmountRecordsOutboundAndInboundTransferTransactions() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(toAccount));
        when(transactionRepository.sumOutboundTransferAmountByAccountIdAndTimestampBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        accountService.transferAmount(fromAccount, "bob", new BigDecimal("300.00"));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());

        assertEquals("Transfer Out to bob", transactionCaptor.getAllValues().get(0).getType());
        assertEquals(fromAccount, transactionCaptor.getAllValues().get(0).getAccount());
        assertEquals("Transfer In from alice", transactionCaptor.getAllValues().get(1).getType());
        assertEquals(toAccount, transactionCaptor.getAllValues().get(1).getAccount());
    }

    private Account account(Long id, String username, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setBalance(new BigDecimal(balance));
        return account;
    }
}
