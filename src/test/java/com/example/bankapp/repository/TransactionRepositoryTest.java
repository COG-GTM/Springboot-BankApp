package com.example.bankapp.repository;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void findByAccountId_withTransactions_returnsList() {
        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword("password");
        account.setBalance(new BigDecimal("100.00"));
        entityManager.persistAndFlush(account);

        Transaction t1 = new Transaction(new BigDecimal("50.00"), "Deposit", LocalDateTime.now(), account);
        Transaction t2 = new Transaction(new BigDecimal("20.00"), "Withdrawal", LocalDateTime.now(), account);
        entityManager.persistAndFlush(t1);
        entityManager.persistAndFlush(t2);

        List<Transaction> result = transactionRepository.findByAccountId(account.getId());

        assertEquals(2, result.size());
    }

    @Test
    void findByAccountId_noTransactions_returnsEmptyList() {
        Account account = new Account();
        account.setUsername("emptyuser");
        account.setPassword("password");
        account.setBalance(BigDecimal.ZERO);
        entityManager.persistAndFlush(account);

        List<Transaction> result = transactionRepository.findByAccountId(account.getId());

        assertTrue(result.isEmpty());
    }
}
