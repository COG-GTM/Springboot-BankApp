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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

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
        account.setId(1L);
        account.setUsername("alice");
        account.setBalance(new BigDecimal("100.00"));
    }

    private Account recipient(String balance) {
        Account bob = new Account();
        bob.setId(2L);
        bob.setUsername("bob");
        bob.setBalance(new BigDecimal(balance));
        lenient().when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        return bob;
    }

    private Transaction txAt(LocalDateTime timestamp) {
        return new Transaction(BigDecimal.ONE, "Deposit", timestamp, account);
    }

    @Test
    void hasSufficientFundsIsTrueWhenBalanceEqualsAmount() {
        assertTrue(accountService.hasSufficientFunds(account, new BigDecimal("100.00")));
    }

    @Test
    void withdrawingTheFullBalanceSucceeds() {
        accountService.withdraw(account, new BigDecimal("100.00"));

        assertEquals(0, account.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void transferringTheFullBalanceSucceeds() {
        Account bob = recipient("0.00");

        accountService.transferAmount(account, "bob", new BigDecimal("100.00"));

        assertEquals(0, account.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, bob.getBalance().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void dateRangeHistoryIncludesTransactionsOnTheEndDay() {
        Transaction onEndDay = txAt(LocalDateTime.of(2026, 11, 18, 14, 30));
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(onEndDay));

        List<Transaction> result = accountService.getTransactionHistory(
                account, LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 18));

        assertEquals(List.of(onEndDay), result);
    }

    @Test
    void singleDayRangeReturnsThatDaysTransactions() {
        Transaction morning = txAt(LocalDateTime.of(2026, 11, 18, 9, 0));
        Transaction lastSecond = txAt(LocalDateTime.of(2026, 11, 18, 23, 59, 59));
        Transaction nextDay = txAt(LocalDateTime.of(2026, 11, 19, 0, 0));
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(nextDay, lastSecond, morning));

        LocalDate day = LocalDate.of(2026, 11, 18);
        List<Transaction> result = accountService.getTransactionHistory(account, day, day);

        assertEquals(List.of(morning, lastSecond), result);
    }

    @Test
    void negativeWithdrawalIsRejectedAndDoesNotIncreaseBalance() {
        assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("-50.00")));
        assertEquals(0, account.getBalance().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void negativeTransferIsRejectedAndDoesNotTakeRecipientFunds() {
        Account bob = recipient("500.00");

        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(account, "bob", new BigDecimal("-200.00")));
        assertEquals(0, account.getBalance().compareTo(new BigDecimal("100.00")));
        assertEquals(0, bob.getBalance().compareTo(new BigDecimal("500.00")));
    }

    @Test
    void negativeDepositIsRejectedAndDoesNotDecreaseBalance() {
        assertThrows(RuntimeException.class,
                () -> accountService.deposit(account, new BigDecimal("-50.00")));
        assertEquals(0, account.getBalance().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void loadUserByUsernameThrowsUsernameNotFoundForUnknownUser() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> accountService.loadUserByUsername("ghost"));
    }
}
