package com.example.bankapp.event;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TransactionEventListenerTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void deposit_publishesEventAndCreatesTransaction() {
        Account account = accountService.registerAccount("eventuser1", "password123");

        accountService.deposit(account, new BigDecimal("500"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Transaction> txns = transactionRepository.findByAccountId(account.getId());
            assertFalse(txns.isEmpty());
            assertEquals("Deposit", txns.get(0).getType());
            assertEquals(0, new BigDecimal("500").compareTo(txns.get(0).getAmount()));
        });
    }

    @Test
    void withdraw_publishesEventAndCreatesTransaction() {
        Account account = accountService.registerAccount("eventuser2", "password123");
        accountService.deposit(account, new BigDecimal("1000"));

        Account updated = accountService.findAccountByUsername("eventuser2");
        accountService.withdraw(updated, new BigDecimal("300"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Transaction> txns = transactionRepository.findByAccountId(account.getId());
            boolean hasWithdrawal = txns.stream()
                    .anyMatch(t -> "Withdrawal".equals(t.getType()));
            assertTrue(hasWithdrawal);
        });
    }

    @Test
    void transfer_publishesEventsAndCreatesTransactions() {
        Account sender = accountService.registerAccount("eventsender", "password123");
        accountService.deposit(sender, new BigDecimal("1000"));

        accountService.registerAccount("eventreceiver", "password123");

        Account senderUpdated = accountService.findAccountByUsername("eventsender");
        accountService.transferAmount(senderUpdated, "eventreceiver", new BigDecimal("200"));

        Account receiver = accountService.findAccountByUsername("eventreceiver");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Transaction> senderTxns = transactionRepository.findByAccountId(sender.getId());
            boolean hasTransferOut = senderTxns.stream()
                    .anyMatch(t -> t.getType().startsWith("Transfer Out"));
            assertTrue(hasTransferOut);

            List<Transaction> receiverTxns = transactionRepository.findByAccountId(receiver.getId());
            boolean hasTransferIn = receiverTxns.stream()
                    .anyMatch(t -> t.getType().startsWith("Transfer In"));
            assertTrue(hasTransferIn);
        });
    }
}
