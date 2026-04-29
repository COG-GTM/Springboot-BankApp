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
    void transferAmount_notAtomic_senderDebitedButRecipientNotCredited() {
        when(accountRepository.findByUsername("recipient")).thenReturn(Optional.of(recipient));

        // First save (sender) succeeds, second save (recipient) throws
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))  // sender save succeeds
                .thenThrow(new RuntimeException("Database error"));   // recipient save fails

        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "recipient", new BigDecimal("200.00")));

        // The sender's balance was deducted (in-memory)
        assertEquals(new BigDecimal("800.00"), sender.getBalance());
        // The recipient never got credited because the save failed
        // but the in-memory object was already mutated
        assertEquals(new BigDecimal("700.00"), recipient.getBalance());

        // The sender's save was called (balance deducted and persisted)
        // but the recipient's save threw an exception — no rollback occurred
        // This demonstrates the atomicity problem: sender loses money, recipient doesn't receive it
        verify(accountRepository, times(2)).save(any(Account.class));
    }
}
