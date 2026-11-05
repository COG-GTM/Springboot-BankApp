package com.example.bankapp.service;

import com.example.bankapp.exception.InvalidTransferException;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.ProcessedTransfer;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.ProcessedTransferRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTransferTest {

    private static final String KEY = "11111111-2222-3333-4444-555555555555";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ProcessedTransferRepository processedTransferRepository;

    @InjectMocks
    private AccountService accountService;

    private Account sender;
    private Account recipient;

    @BeforeEach
    void setUp() {
        sender = account("alice", new BigDecimal("100.00"));
        recipient = account("bob", new BigDecimal("50.00"));
    }

    private Account account(String username, BigDecimal balance) {
        Account account = new Account();
        account.setUsername(username);
        account.setBalance(balance);
        return account;
    }

    @Test
    void transferMovesFundsAndRecordsIdempotencyKey() {
        when(processedTransferRepository.existsByIdempotencyKey(KEY)).thenReturn(false);
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));

        accountService.transferAmount(sender, "bob", new BigDecimal("25.00"), KEY);

        assertThat(sender.getBalance()).isEqualByComparingTo("75.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("75.00");
        verify(processedTransferRepository).saveAndFlush(any(ProcessedTransfer.class));
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", new BigDecimal("-10.00"), KEY))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Transfer amount must be greater than zero");

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verifyNoInteractions(accountRepository, transactionRepository, processedTransferRepository);
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", BigDecimal.ZERO, KEY))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Transfer amount must be greater than zero");

        verifyNoInteractions(accountRepository, transactionRepository, processedTransferRepository);
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", null, KEY))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Transfer amount is required");
    }

    @Test
    void rejectsSubCentAmount() {
        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", new BigDecimal("0.001"), KEY))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Transfer amount cannot have more than two decimal places");
    }

    @Test
    void rejectsAmountAbovePerTransactionLimit() {
        BigDecimal overLimit = AccountService.MAX_TRANSFER_AMOUNT.add(BigDecimal.ONE);

        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", overLimit, KEY))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Transfer amount exceeds the per-transaction limit");
    }

    @Test
    void rejectsSelfTransferIgnoringCaseAndWhitespace() {
        assertThatThrownBy(() -> accountService.transferAmount(sender, "  ALICE ", new BigDecimal("10.00"), KEY))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Cannot transfer to your own account");

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verifyNoInteractions(accountRepository, transactionRepository, processedTransferRepository);
    }

    @Test
    void rejectsBlankRecipient() {
        assertThatThrownBy(() -> accountService.transferAmount(sender, "   ", new BigDecimal("10.00"), KEY))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Recipient username is required");
    }

    @Test
    void rejectsMissingIdempotencyKey() {
        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", new BigDecimal("10.00"), null))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Missing transfer request identifier");

        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void rejectsDuplicateSubmission() {
        when(processedTransferRepository.existsByIdempotencyKey(KEY)).thenReturn(true);

        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", new BigDecimal("10.00"), KEY))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Duplicate transfer request ignored");

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsConcurrentDuplicateLosingTheUniqueConstraintRace() {
        when(processedTransferRepository.existsByIdempotencyKey(KEY)).thenReturn(false);
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));
        when(processedTransferRepository.saveAndFlush(any(ProcessedTransfer.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", new BigDecimal("10.00"), KEY))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessage("Duplicate transfer request ignored");

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsTransferExceedingBalance() {
        when(processedTransferRepository.existsByIdempotencyKey(KEY)).thenReturn(false);

        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", new BigDecimal("500.00"), KEY))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");

        verify(processedTransferRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsUnknownRecipient() {
        when(processedTransferRepository.existsByIdempotencyKey(KEY)).thenReturn(false);
        when(accountRepository.findByUsername("carol")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transferAmount(sender, "carol", new BigDecimal("10.00"), KEY))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Recipient account not found");

        verify(processedTransferRepository, never()).saveAndFlush(any());
    }
}
