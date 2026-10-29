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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
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
class AccountServiceTransferTest {

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

    private List<Transaction> captureSavedTransactions(int expected) {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(expected)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void happyPathDebitsSenderCreditsRecipientAndRecordsBothTransactions() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "bob", new BigDecimal("30.00"));

        assertThat(sender.getBalance()).isEqualByComparingTo("70.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("80.00");

        InOrder order = inOrder(accountRepository, transactionRepository);
        order.verify(accountRepository).findByUsername("bob");
        order.verify(accountRepository).save(sender);
        order.verify(accountRepository).save(recipient);
        order.verify(transactionRepository, times(2)).save(any(Transaction.class));
        order.verifyNoMoreInteractions();

        List<Transaction> saved = captureSavedTransactions(2);
        Transaction debit = saved.get(0);
        Transaction credit = saved.get(1);

        assertThat(debit.getType()).isEqualTo("Transfer Out to bob");
        assertThat(debit.getAccount()).isSameAs(sender);
        assertThat(debit.getAmount()).isEqualByComparingTo("30.00");
        assertThat(debit.getTimestamp()).isNotNull();

        assertThat(credit.getType()).isEqualTo("Transfer In from alice");
        assertThat(credit.getAccount()).isSameAs(recipient);
        assertThat(credit.getAmount()).isEqualByComparingTo("30.00");
        assertThat(credit.getTimestamp()).isNotNull();
    }

    @Test
    void insufficientFundsThrowsAndChangesNothing() {
        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", new BigDecimal("100.01")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("50.00");
        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void transferOfEntireBalanceSucceedsAndLeavesSenderAtZero() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "bob", new BigDecimal("100.00"));

        assertThat(sender.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(recipient.getBalance()).isEqualByComparingTo("150.00");
        verify(accountRepository).save(sender);
        verify(accountRepository).save(recipient);
        captureSavedTransactions(2);
    }

    @Test
    void unknownRecipientThrowsAndSavesNothing() {
        when(accountRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transferAmount(sender, "nobody", new BigDecimal("10.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Recipient account not found");

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void balanceCheckRunsBeforeRecipientLookup() {
        // LIKELY BUG: an over-balance transfer to a non-existent user reports "Insufficient funds", masking "Recipient account not found"; recipient should be validated first.
        assertThatThrownBy(() -> accountService.transferAmount(sender, "nobody", new BigDecimal("500.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");

        verify(accountRepository, never()).findByUsername(anyString());
        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void negativeAmountIsAcceptedAndReversesTheFlowOfMoney() {
        // LIKELY BUG: no amount validation; a negative transfer pulls money from the recipient into the sender.
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "bob", new BigDecimal("-25.00"));

        assertThat(sender.getBalance()).isEqualByComparingTo("125.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("25.00");
        List<Transaction> saved = captureSavedTransactions(2);
        assertThat(saved).allSatisfy(t -> assertThat(t.getAmount()).isEqualByComparingTo("-25.00"));
    }

    @Test
    void zeroAmountIsAcceptedAndRecordsTwoZeroValueTransactions() {
        // LIKELY BUG: no amount validation; a zero transfer succeeds and writes two meaningless transaction rows.
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "bob", BigDecimal.ZERO);

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("50.00");
        verify(accountRepository).save(sender);
        verify(accountRepository).save(recipient);
        List<Transaction> saved = captureSavedTransactions(2);
        assertThat(saved).allSatisfy(t -> assertThat(t.getAmount()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    void selfTransferIsAllowedAndRecordsTwoTransactions() {
        // LIKELY BUG: no self-transfer check; transferring to yourself succeeds and writes a pair of Out/In rows on the same account.
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(sender));

        accountService.transferAmount(sender, "alice", new BigDecimal("40.00"));

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, times(2)).save(sender);
        List<Transaction> saved = captureSavedTransactions(2);
        assertThat(saved).extracting(Transaction::getType)
                .containsExactly("Transfer Out to alice", "Transfer In from alice");
        assertThat(saved).allSatisfy(t -> assertThat(t.getAccount()).isSameAs(sender));
    }

    @Test
    void recipientSaveFailureLeavesSenderAlreadyDebitedAndSaved() {
        // LIKELY BUG: transferAmount is not @Transactional, so a failure after save(fromAccount) leaves the debit committed.
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            if (inv.getArgument(0) == recipient) {
                throw new RuntimeException("DB down");
            }
            return inv.getArgument(0);
        });

        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", new BigDecimal("30.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB down");

        assertThat(sender.getBalance()).isEqualByComparingTo("70.00");
        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository).save(sender);
        order.verify(accountRepository).save(recipient);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void creditTransactionSaveFailureLeavesBothBalancesAndDebitRecordSaved() {
        // LIKELY BUG: transferAmount is not @Transactional, so a failure saving the credit row leaves a half-recorded transfer.
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getType().startsWith("Transfer In")) {
                throw new RuntimeException("DB down");
            }
            return t;
        });

        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", new BigDecimal("30.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB down");

        assertThat(sender.getBalance()).isEqualByComparingTo("70.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("80.00");
        verify(accountRepository).save(sender);
        verify(accountRepository).save(recipient);
        List<Transaction> attempted = captureSavedTransactions(2);
        assertThat(attempted).extracting(Transaction::getType)
                .containsExactly("Transfer Out to bob", "Transfer In from alice");
    }

    @Test
    void transferAmountHasNoTransactionalAnnotation() throws NoSuchMethodException {
        // LIKELY BUG: neither the method nor the class is @Transactional, so the multi-row transfer is not atomic.
        Method method = AccountService.class.getMethod(
                "transferAmount", Account.class, String.class, BigDecimal.class);

        assertThat(Arrays.stream(method.getAnnotations()).map(Annotation::annotationType).map(Class::getSimpleName))
                .doesNotContain("Transactional");
        assertThat(Arrays.stream(AccountService.class.getAnnotations()).map(Annotation::annotationType).map(Class::getSimpleName))
                .doesNotContain("Transactional");
    }

    @Test
    void nullAmountThrowsNullPointerException() {
        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", null))
                .isInstanceOf(NullPointerException.class);

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verifyNoInteractions(accountRepository, transactionRepository);
    }
}
