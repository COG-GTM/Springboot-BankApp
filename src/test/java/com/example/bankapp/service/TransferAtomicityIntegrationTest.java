package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

/**
 * Control APP-TXN-02: a transfer must post both legs inside one transaction, so a failure
 * after the debit rolls the whole operation back and no money is destroyed.
 */
@SpringBootTest
class TransferAtomicityIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @SpyBean
    private TransactionRepository transactionRepository;

    private Account sender;
    private Account recipient;

    @BeforeEach
    void setUp() {
        sender = newAccount("atomicity-sender-" + UUID.randomUUID(), new BigDecimal("500.00"));
        recipient = newAccount("atomicity-recipient-" + UUID.randomUUID(), new BigDecimal("100.00"));
    }

    private Account newAccount(String username, BigDecimal balance) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("{noop}irrelevant");
        account.setBalance(balance);
        return accountRepository.save(account);
    }

    @Test
    void transferRollsBackBothLegsWhenTheCreditLegFails() {
        doThrow(new RuntimeException("simulated ledger outage"))
                .when(transactionRepository)
                .save(argThat((Transaction t) -> t != null && t.getType() != null && t.getType().startsWith("Transfer In")));

        assertThatThrownBy(() -> accountService.transferAmount(sender, recipient.getUsername(), new BigDecimal("250.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("simulated ledger outage");

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(accountRepository.findById(sender.getId()).orElseThrow().getBalance())
                    .isEqualByComparingTo("500.00");
            assertThat(accountRepository.findById(recipient.getId()).orElseThrow().getBalance())
                    .isEqualByComparingTo("100.00");
            assertThat(transactionRepository.findByAccountId(sender.getId())).isEmpty();
            assertThat(transactionRepository.findByAccountId(recipient.getId())).isEmpty();
        });
    }

    @Test
    void successfulTransferPostsBothLegs() {
        accountService.transferAmount(sender, recipient.getUsername(), new BigDecimal("250.00"));

        assertThat(accountRepository.findById(sender.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("250.00");
        assertThat(accountRepository.findById(recipient.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("350.00");
        assertThat(transactionRepository.findByAccountId(sender.getId())).hasSize(1);
        assertThat(transactionRepository.findByAccountId(recipient.getId())).hasSize(1);
    }
}
