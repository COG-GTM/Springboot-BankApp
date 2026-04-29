package com.example.bankapp.event;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventListenerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionEventListener listener;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUsername("testuser");
        testAccount.setBalance(new BigDecimal("1000"));
    }

    @Test
    void handleTransactionEvent_createsTransaction() {
        TransactionEvent event = new TransactionEvent(new BigDecimal("500"), "Deposit", 1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.handleTransactionEvent(event);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertEquals(new BigDecimal("500"), saved.getAmount());
        assertEquals("Deposit", saved.getType());
        assertEquals(testAccount, saved.getAccount());
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void handleTransactionEvent_accountNotFound() {
        TransactionEvent event = new TransactionEvent(new BigDecimal("500"), "Deposit", 99L);
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> listener.handleTransactionEvent(event));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
