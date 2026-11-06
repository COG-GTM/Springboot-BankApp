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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceDailyWithdrawalLimitTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(accountService, "dailyWithdrawalLimit", new BigDecimal("5000"));
        account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("10000"));
    }

    private void givenTodaysTransactions(Transaction... transactions) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        when(transactionRepository.findByAccountIdAndTimestampBetween(1L, startOfDay, startOfDay.plusDays(1)))
                .thenReturn(List.of(transactions));
    }

    private Transaction today(String amount, String type) {
        return new Transaction(new BigDecimal(amount), type, LocalDateTime.now(), account);
    }

    @Test
    void allowsWithdrawalsUpToTheDailyLimit() {
        givenTodaysTransactions(today("3000", "Withdrawal"));

        accountService.withdraw(account, new BigDecimal("2000"));

        assertEquals(new BigDecimal("8000"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void rejectsWithdrawalThatPushesTodaysTotalOverTheLimit() {
        givenTodaysTransactions(today("3000", "Withdrawal"), today("1500", "Withdrawal"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("500.01")));

        assertEquals("Daily withdrawal limit of $5000 exceeded", ex.getMessage());
        assertEquals(new BigDecimal("10000"), account.getBalance());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsSingleWithdrawalOverTheLimit() {
        givenTodaysTransactions();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("5001")));

        assertEquals("Daily withdrawal limit of $5000 exceeded", ex.getMessage());
    }

    @Test
    void ignoresDepositsAndTransfersWhenSummingTodaysWithdrawals() {
        givenTodaysTransactions(
                today("4000", "Deposit"),
                today("4000", "Transfer Out to bob"),
                today("1000", "Withdrawal"));

        accountService.withdraw(account, new BigDecimal("4000"));

        assertEquals(new BigDecimal("6000"), account.getBalance());
    }

    @Test
    void keepsInsufficientFundsCheckWithinTheLimit() {
        account.setBalance(new BigDecimal("100"));
        givenTodaysTransactions();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(account, new BigDecimal("200")));

        assertEquals("Insufficient funds", ex.getMessage());
        verify(transactionRepository, never()).save(any());
        verify(transactionRepository).findByAccountIdAndTimestampBetween(eq(1L), any(), any());
    }
}
