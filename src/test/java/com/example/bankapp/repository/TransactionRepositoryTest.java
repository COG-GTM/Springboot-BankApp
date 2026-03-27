package com.example.bankapp.repository;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void findByAccountId_withTransactions_returnsList() {
        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword("password");
        account.setBalance(new BigDecimal("100.00"));
        account = accountRepository.save(account);

        Transaction tx1 = new Transaction(new BigDecimal("50.00"), "Deposit",
                LocalDateTime.now(), account);
        Transaction tx2 = new Transaction(new BigDecimal("25.00"), "Withdrawal",
                LocalDateTime.now(), account);
        transactionRepository.save(tx1);
        transactionRepository.save(tx2);

        List<Transaction> transactions = transactionRepository.findByAccountId(account.getId());

        assertEquals(2, transactions.size());
    }

    @Test
    void findByAccountId_noTransactions_returnsEmptyList() {
        Account account = new Account();
        account.setUsername("emptyuser");
        account.setPassword("password");
        account.setBalance(BigDecimal.ZERO);
        account = accountRepository.save(account);

        List<Transaction> transactions = transactionRepository.findByAccountId(account.getId());

        assertTrue(transactions.isEmpty());
    }
}
