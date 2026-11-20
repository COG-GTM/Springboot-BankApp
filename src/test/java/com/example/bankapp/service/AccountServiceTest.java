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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private Account alice;
    private Account bob;

    @BeforeEach
    void setUp() {
        alice = account(1L, "alice", "100.00");
        bob = account(2L, "bob", "50.00");
        lenient().when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    }

    private static Account account(Long id, String username, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    @Test
    void hasSufficientFundsIsTrueWhenAmountEqualsBalance() {
        assertThat(accountService.hasSufficientFunds(alice, new BigDecimal("100.00"))).isTrue();
    }

    @Test
    void withdrawingTheFullBalanceLeavesZero() {
        accountService.withdraw(alice, new BigDecimal("100.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void transferringTheFullBalanceSucceeds() {
        accountService.transferAmount(alice, "bob", new BigDecimal("100.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("0");
        assertThat(bob.getBalance()).isEqualByComparingTo("150.00");
    }

    @Test
    void dateRangeHistoryIncludesTransactionsOnTheEndDay() {
        Transaction onStartDay = new Transaction(BigDecimal.TEN, "Deposit", LocalDateTime.of(2026, 11, 1, 9, 0), alice);
        Transaction onEndDay = new Transaction(BigDecimal.ONE, "Deposit", LocalDateTime.of(2026, 11, 3, 15, 30), alice);
        Transaction afterRange = new Transaction(BigDecimal.ONE, "Deposit", LocalDateTime.of(2026, 11, 4, 0, 0), alice);
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(onEndDay, afterRange, onStartDay));

        List<Transaction> history = accountService.getTransactionHistory(
                alice, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3));

        assertThat(history).containsExactly(onStartDay, onEndDay);
    }

    @Test
    void singleDayHistoryReturnsThatDaysTransactions() {
        Transaction sameDay = new Transaction(BigDecimal.TEN, "Deposit", LocalDateTime.of(2026, 11, 2, 12, 0), alice);
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(sameDay));

        LocalDate day = LocalDate.of(2026, 11, 2);
        assertThat(accountService.getTransactionHistory(alice, day, day)).containsExactly(sameDay);
    }

    @Test
    void loadUserByUsernameThrowsUsernameNotFoundForUnknownUser() {
        when(accountRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void depositRejectsNegativeAmount() {
        assertThatThrownBy(() -> accountService.deposit(alice, new BigDecimal("-30.00")))
                .isInstanceOf(RuntimeException.class);
        assertThat(alice.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void withdrawRejectsNegativeAmount() {
        assertThatThrownBy(() -> accountService.withdraw(alice, new BigDecimal("-30.00")))
                .isInstanceOf(RuntimeException.class);
        assertThat(alice.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void transferRejectsNegativeAmount() {
        assertThatThrownBy(() -> accountService.transferAmount(alice, "bob", new BigDecimal("-30.00")))
                .isInstanceOf(RuntimeException.class);
        assertThat(alice.getBalance()).isEqualByComparingTo("100.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("50.00");
    }
}
