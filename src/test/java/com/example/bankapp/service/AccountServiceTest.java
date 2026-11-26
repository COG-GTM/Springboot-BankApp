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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private static Account account(long id, String username, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    @Test
    void hasSufficientFundsIsTrueWhenBalanceEqualsAmount() {
        Account account = account(1L, "alice", "100.00");

        assertThat(accountService.hasSufficientFunds(account, new BigDecimal("100.00"))).isTrue();
    }

    @Test
    void withdrawingTheFullBalanceSucceeds() {
        Account account = account(1L, "alice", "100.00");

        accountService.withdraw(account, new BigDecimal("100.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void transferringTheFullBalanceSucceeds() {
        Account alice = account(1L, "alice", "100.00");
        Account bob = account(2L, "bob", "0.00");
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        accountService.transferAmount(alice, "bob", new BigDecimal("100.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("0");
        assertThat(bob.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void transactionHistoryRangeIncludesTransactionsOnTheEndDay() {
        Account account = account(1L, "alice", "0");
        Transaction startDay = new Transaction(BigDecimal.ONE, "Deposit", LocalDateTime.of(2026, 11, 1, 0, 0), account);
        Transaction endDay = new Transaction(BigDecimal.TEN, "Deposit", LocalDateTime.of(2026, 11, 3, 14, 30), account);
        Transaction afterRange = new Transaction(BigDecimal.TEN, "Deposit", LocalDateTime.of(2026, 11, 4, 0, 0), account);
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(afterRange, endDay, startDay));

        List<Transaction> history = accountService.getTransactionHistory(
                account, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3));

        assertThat(history).containsExactly(startDay, endDay);
    }

    @Test
    void singleDayTransactionHistoryRangeIsNotEmpty() {
        Account account = account(1L, "alice", "0");
        Transaction sameDay = new Transaction(BigDecimal.ONE, "Deposit", LocalDateTime.of(2026, 11, 2, 9, 0), account);
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(sameDay));

        LocalDate day = LocalDate.of(2026, 11, 2);

        assertThat(accountService.getTransactionHistory(account, day, day)).containsExactly(sameDay);
    }

    @Test
    void transferWithNegativeAmountIsRejected() {
        Account alice = account(1L, "alice", "0.00");
        Account bob = account(2L, "bob", "500.00");
        lenient().when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        assertThatThrownBy(() -> accountService.transferAmount(alice, "bob", new BigDecimal("-500.00")))
                .isInstanceOf(RuntimeException.class);
        assertThat(alice.getBalance()).isEqualByComparingTo("0.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void withdrawWithNegativeAmountIsRejected() {
        Account alice = account(1L, "alice", "0.00");

        assertThatThrownBy(() -> accountService.withdraw(alice, new BigDecimal("-50.00")))
                .isInstanceOf(RuntimeException.class);
        assertThat(alice.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void depositWithNegativeAmountIsRejected() {
        Account alice = account(1L, "alice", "0.00");

        assertThatThrownBy(() -> accountService.deposit(alice, new BigDecimal("-50.00")))
                .isInstanceOf(RuntimeException.class);
        assertThat(alice.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void loadUserByUsernameThrowsUsernameNotFoundExceptionForUnknownUser() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
