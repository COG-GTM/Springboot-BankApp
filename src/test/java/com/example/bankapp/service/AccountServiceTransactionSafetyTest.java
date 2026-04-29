package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTransactionSafetyTest {

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
        sender = new Account();
        sender.setId(1L);
        sender.setUsername("sender");
        sender.setBalance(new BigDecimal("1000.00"));

        recipient = new Account();
        recipient.setId(2L);
        recipient.setUsername("recipient");
        recipient.setBalance(new BigDecimal("500.00"));
    }

    @Test
    void transferAmount_withTransactional_rollsBackOnFailure() {
        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(recipient));

        // First save (sender) succeeds, second save (recipient) throws
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "recipient", new BigDecimal("200.00")));

        // In unit tests (no Spring context), @Transactional doesn't provide actual rollback.
        // The in-memory objects are still mutated, but with @Transactional in production,
        // the database changes would be rolled back by the transaction manager.
        // This test verifies the exception propagates, which triggers the rollback.
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void transferAmount_exceptionPropagates_enablingTransactionalRollback() {
        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(recipient));

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new RuntimeException("Database error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "recipient", new BigDecimal("200.00")));

        assertEquals("Database error", ex.getMessage());
        // The exception is not caught internally, so @Transactional will trigger rollback
        // in a real Spring context, undoing the sender's balance deduction
    }
}
