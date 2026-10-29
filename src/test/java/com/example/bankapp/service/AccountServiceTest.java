package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        account.setPassword("encoded");
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    private List<Transaction> capturedTransactions(int expectedCount) {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(expectedCount)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void findAccountByUsernameReturnsAccountWhenPresent() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(sender));

        assertThat(accountService.findAccountByUsername("alice")).isSameAs(sender);
    }

    @Test
    void findAccountByUsernameThrowsWhenMissing() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findAccountByUsername("ghost"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account not found");
    }

    @Test
    void transferMovesMoneyAndRecordsBothTransactions() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "bob", new BigDecimal("30.00"));

        assertThat(sender.getBalance()).isEqualByComparingTo("70.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("80.00");

        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository).save(sender);
        order.verify(accountRepository).save(recipient);
        verify(accountRepository, times(2)).save(any(Account.class));

        List<Transaction> transactions = capturedTransactions(2);
        Transaction out = transactions.get(0);
        assertThat(out.getType()).isEqualTo("Transfer Out to bob");
        assertThat(out.getAmount()).isEqualByComparingTo("30.00");
        assertThat(out.getAccount()).isSameAs(sender);
        assertThat(out.getTimestamp()).isNotNull();

        Transaction in = transactions.get(1);
        assertThat(in.getType()).isEqualTo("Transfer In from alice");
        assertThat(in.getAmount()).isEqualByComparingTo("30.00");
        assertThat(in.getAccount()).isSameAs(recipient);
        assertThat(in.getTimestamp()).isNotNull();
    }

    @Test
    void transferOfEntireBalanceSucceedsAndLeavesSenderAtZero() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "bob", new BigDecimal("100.00"));

        assertThat(sender.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(recipient.getBalance()).isEqualByComparingTo("150.00");
        verify(accountRepository).save(sender);
        verify(accountRepository).save(recipient);
        capturedTransactions(2);
    }

    @Test
    void transferWithInsufficientFundsThrowsAndSavesNothing() {
        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", new BigDecimal("100.01")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("50.00");
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void transferToUnknownRecipientThrowsAndSavesNothing() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transferAmount(sender, "ghost", new BigDecimal("30.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Recipient account not found");

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void insufficientFundsIsCheckedBeforeRecipientLookup() {
        assertThatThrownBy(() -> accountService.transferAmount(sender, "ghost", new BigDecimal("500.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");

        verify(accountRepository, never()).findByUsername(anyString());
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void transferOfOneCentKeepsExactPrecision() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "bob", new BigDecimal("0.01"));

        assertThat(sender.getBalance()).isEqualTo(new BigDecimal("99.99"));
        assertThat(recipient.getBalance()).isEqualTo(new BigDecimal("50.01"));
        assertThat(capturedTransactions(2))
                .extracting(Transaction::getAmount)
                .containsExactly(new BigDecimal("0.01"), new BigDecimal("0.01"));
    }

    @Test
    void transferWithThreeDecimalPlacesKeepsExactPrecision() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "bob", new BigDecimal("12.345"));

        assertThat(sender.getBalance()).isEqualTo(new BigDecimal("87.655"));
        assertThat(recipient.getBalance()).isEqualTo(new BigDecimal("62.345"));
        assertThat(capturedTransactions(2))
                .extracting(Transaction::getAmount)
                .containsExactly(new BigDecimal("12.345"), new BigDecimal("12.345"));
    }

    @Test
    void negativeAmountIsAcceptedAndMovesMoneyFromRecipientToSender() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        // LIKELY BUG: no amount validation, so a negative transfer pulls money from the recipient.
        accountService.transferAmount(sender, "bob", new BigDecimal("-50.00"));

        assertThat(sender.getBalance()).isEqualByComparingTo("150.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("0.00");
        assertThat(capturedTransactions(2))
                .extracting(Transaction::getAmount)
                .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("-50.00"));
    }

    @Test
    void zeroAmountIsAcceptedAndStillWritesTwoTransactions() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        // LIKELY BUG: a zero transfer is not rejected and pollutes both histories.
        accountService.transferAmount(sender, "bob", BigDecimal.ZERO);

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("50.00");
        assertThat(capturedTransactions(2))
                .extracting(Transaction::getType)
                .containsExactly("Transfer Out to bob", "Transfer In from alice");
    }

    @Test
    void selfTransferIsAcceptedAndWritesTransferRecords() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(sender));

        // LIKELY BUG: no self-transfer check; the account transfers to itself and gets Out/In records.
        accountService.transferAmount(sender, "alice", new BigDecimal("30.00"));

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, times(2)).save(sender);
        List<Transaction> transactions = capturedTransactions(2);
        assertThat(transactions).extracting(Transaction::getType)
                .containsExactly("Transfer Out to alice", "Transfer In from alice");
        assertThat(transactions).extracting(Transaction::getAccount)
                .containsExactly(sender, sender);
    }

    @Test
    void failureSavingRecipientLeavesSenderDebitedAndSaved() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            if (saved == recipient) {
                throw new RuntimeException("DB down");
            }
            return saved;
        });

        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", new BigDecimal("30.00")))
                .hasMessage("DB down");

        // LIKELY BUG: transferAmount is not @Transactional, so the sender's debit is already persisted with no rollback.
        verify(accountRepository).save(sender);
        assertThat(sender.getBalance()).isEqualByComparingTo("70.00");
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void nullAmountThrowsNullPointerException() {
        // LIKELY BUG: a null amount surfaces as a NullPointerException instead of a friendly validation error.
        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", null))
                .isInstanceOf(NullPointerException.class);

        verify(accountRepository, never()).findByUsername(anyString());
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(transactionRepository);
    }
}
