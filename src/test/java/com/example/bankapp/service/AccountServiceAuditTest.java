package com.example.bankapp.service;

import com.example.bankapp.audit.AuditEvent;
import com.example.bankapp.audit.AuditLogger;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceAuditTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AccountService accountService;

    @Test
    void depositIsAudited() {
        Account account = account(1L, "alice", "100.00");

        accountService.deposit(account, new BigDecimal("50.00"));

        AuditEvent event = capturedEvent();
        assertThat(event.eventType()).isEqualTo("DEPOSIT");
        assertThat(event.outcome()).isEqualTo(AuditEvent.Outcome.SUCCESS);
    }

    @Test
    void rejectedWithdrawalIsAuditedAsAFailure() {
        Account account = account(1L, "alice", "10.00");

        assertThatThrownBy(() -> accountService.withdraw(account, new BigDecimal("50.00")))
                .isInstanceOf(RuntimeException.class);

        AuditEvent event = capturedEvent();
        assertThat(event.eventType()).isEqualTo("WITHDRAWAL");
        assertThat(event.outcome()).isEqualTo(AuditEvent.Outcome.FAILURE);
    }

    @Test
    void transferIsAuditedWithBothSidesOfTheMovement() {
        Account from = account(1L, "alice", "100.00");
        Account to = account(2L, "bob", "0.00");
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(to));

        accountService.transferAmount(from, "bob", new BigDecimal("40.00"));

        AuditEvent event = capturedEvent();
        assertThat(event.eventType()).isEqualTo("TRANSFER");
        assertThat(event.outcome()).isEqualTo(AuditEvent.Outcome.SUCCESS);
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(any(Transaction.class));
    }

    @Test
    void transferToAnUnknownRecipientIsAuditedAsAFailure() {
        Account from = account(1L, "alice", "100.00");
        when(accountRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transferAmount(from, "nobody", new BigDecimal("40.00")))
                .isInstanceOf(RuntimeException.class);

        AuditEvent event = capturedEvent();
        assertThat(event.eventType()).isEqualTo("TRANSFER");
        assertThat(event.outcome()).isEqualTo(AuditEvent.Outcome.FAILURE);
    }

    private AuditEvent capturedEvent() {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditLogger).log(captor.capture());
        return captor.getValue();
    }

    private Account account(Long id, String username, String balance) {
        Account account = new Account(username, "irrelevant-hash", new BigDecimal(balance),
                Collections.emptyList(), Collections.emptyList());
        account.setId(id);
        return account;
    }
}
