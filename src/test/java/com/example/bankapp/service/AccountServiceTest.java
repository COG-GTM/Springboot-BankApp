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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    private Account sender;
    private Account recipient;

    @BeforeEach
    void setUp() {
        sender = account(1L, "alice", "100.00");
        recipient = account(2L, "bob", "50.00");
    }

    private static Account account(Long id, String username, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    @Test
    void deposit_increasesBalanceAndSavesAccount() {
        accountService.deposit(sender, new BigDecimal("25.50"));

        assertThat(sender.getBalance()).isEqualByComparingTo("125.50");
        verify(accountRepository).save(sender);
    }

    @Test
    void deposit_savesDepositTransactionWithAmountAndAccount() {
        BigDecimal amount = new BigDecimal("25.50");

        accountService.deposit(sender, amount);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("Deposit");
        assertThat(saved.getAmount()).isEqualByComparingTo(amount);
        assertThat(saved.getAccount()).isSameAs(sender);
        assertThat(saved.getTimestamp()).isNotNull();
        verify(accountRepository).save(sender);
        verifyNoMoreInteractions(accountRepository, transactionRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void withdraw_decrementsBalanceAndSavesWithdrawalTransaction() {
        BigDecimal amount = new BigDecimal("40.00");

        accountService.withdraw(sender, amount);

        assertThat(sender.getBalance()).isEqualByComparingTo("60.00");
        verify(accountRepository).save(sender);
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("Withdrawal");
        assertThat(saved.getAmount()).isEqualByComparingTo(amount);
        assertThat(saved.getAccount()).isSameAs(sender);
        verifyNoMoreInteractions(accountRepository, transactionRepository);
    }

    @Test
    void withdraw_exactBalanceLeavesZero() {
        accountService.withdraw(sender, new BigDecimal("100.00"));

        assertThat(sender.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(accountRepository).save(sender);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_insufficientFundsThrowsAndSavesNothing() {
        assertThatThrownBy(() -> accountService.withdraw(sender, new BigDecimal("100.01")))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void transferAmount_debitsSenderCreditsRecipientAndSavesBothAccounts() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "bob", new BigDecimal("30.00"));

        assertThat(sender.getBalance()).isEqualByComparingTo("70.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("80.00");
        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository).findByUsername("bob");
        order.verify(accountRepository).save(sender);
        order.verify(accountRepository).save(recipient);
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void transferAmount_savesTransferOutAndTransferInTransactions() {
        BigDecimal amount = new BigDecimal("30.00");
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "bob", amount);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        List<Transaction> saved = captor.getAllValues();

        Transaction debit = saved.get(0);
        assertThat(debit.getType()).isEqualTo("Transfer Out to bob");
        assertThat(debit.getAmount()).isEqualByComparingTo(amount);
        assertThat(debit.getAccount()).isSameAs(sender);

        Transaction credit = saved.get(1);
        assertThat(credit.getType()).isEqualTo("Transfer In from alice");
        assertThat(credit.getAmount()).isEqualByComparingTo(amount);
        assertThat(credit.getAccount()).isSameAs(recipient);
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    void transferAmount_insufficientFundsThrowsAndSavesNothing() {
        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", new BigDecimal("150.00")))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("50.00");
        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void transferAmount_unknownRecipientThrowsAndSavesNothing() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transferAmount(sender, "ghost", new BigDecimal("10.00")))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Recipient account not found");

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository).findByUsername("ghost");
        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(transactionRepository);
    }
}
