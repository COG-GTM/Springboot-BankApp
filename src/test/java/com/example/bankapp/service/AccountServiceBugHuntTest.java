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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceBugHuntTest {

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

    // Bug 1: hasSufficientFunds is documented as "at least" but uses a strict > comparison.
    @Test
    void hasSufficientFunds_isTrueWhenBalanceEqualsAmount() {
        assertThat(accountService.hasSufficientFunds(alice, new BigDecimal("100.00"))).isTrue();
    }

    @Test
    void withdraw_entireBalance_succeeds() {
        accountService.withdraw(alice, new BigDecimal("100.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void transfer_entireBalance_succeeds() {
        accountService.transferAmount(alice, "bob", new BigDecimal("100.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo("0");
        assertThat(bob.getBalance()).isEqualByComparingTo("150.00");
    }

    // Bug 2: the date-range history is documented as inclusive of both days but drops the whole "to" day.
    @Test
    void transactionHistoryByDate_includesTransactionsOnTheToDay() {
        LocalDate day = LocalDate.of(2026, 11, 18);
        Transaction morning = new Transaction(BigDecimal.TEN, "Deposit", day.atTime(9, 0), alice);
        Transaction evening = new Transaction(BigDecimal.ONE, "Withdrawal", day.atTime(23, 59, 59), alice);
        Transaction nextDay = new Transaction(BigDecimal.ONE, "Deposit", day.plusDays(1).atStartOfDay(), alice);
        when(transactionRepository.findByAccountId(1L)).thenReturn(List.of(evening, nextDay, morning));

        List<Transaction> result = accountService.getTransactionHistory(alice, day, day);

        assertThat(result).containsExactly(morning, evening);
    }

    // Bug 3: non-positive amounts are accepted and move money in the wrong direction.
    @Test
    void deposit_negativeAmount_isRejected() {
        assertThatThrownBy(() -> accountService.deposit(alice, new BigDecimal("-50.00")))
                .isInstanceOf(RuntimeException.class);
        assertThat(alice.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_negativeAmount_isRejected() {
        assertThatThrownBy(() -> accountService.withdraw(alice, new BigDecimal("-50.00")))
                .isInstanceOf(RuntimeException.class);
        assertThat(alice.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_negativeAmount_doesNotPullMoneyFromRecipient() {
        assertThatThrownBy(() -> accountService.transferAmount(alice, "bob", new BigDecimal("-30.00")))
                .isInstanceOf(RuntimeException.class);
        assertThat(alice.getBalance()).isEqualByComparingTo("100.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("50.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_zeroAmount_isRejected() {
        assertThatThrownBy(() -> accountService.transferAmount(alice, "bob", BigDecimal.ZERO))
                .isInstanceOf(RuntimeException.class);
        verify(transactionRepository, never()).save(any());
    }

    // Bug 4: UserDetailsService contract requires UsernameNotFoundException for unknown users.
    @Test
    void loadUserByUsername_unknownUser_throwsUsernameNotFoundException() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
