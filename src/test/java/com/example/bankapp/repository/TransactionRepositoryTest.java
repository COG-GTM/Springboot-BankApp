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
    void findByAccountId_found() {
        Account account = new Account();
        account.setUsername("user1");
        account.setPassword("pass");
        account.setBalance(BigDecimal.TEN);
        account = accountRepository.save(account);

        Transaction transaction = new Transaction(BigDecimal.ONE, "Deposit", LocalDateTime.now(), account);
        transactionRepository.save(transaction);

        List<Transaction> result = transactionRepository.findByAccountId(account.getId());
        assertEquals(1, result.size());
        assertEquals("Deposit", result.get(0).getType());
    }

    @Test
    void findByAccountId_empty() {
        List<Transaction> result = transactionRepository.findByAccountId(999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void saveAndFindById() {
        Account account = new Account();
        account.setUsername("user2");
        account.setPassword("pass");
        account.setBalance(BigDecimal.ZERO);
        account = accountRepository.save(account);

        Transaction transaction = new Transaction(new BigDecimal("100"), "Withdrawal", LocalDateTime.now(), account);
        Transaction saved = transactionRepository.save(transaction);

        assertNotNull(saved.getId());
        assertTrue(transactionRepository.findById(saved.getId()).isPresent());
    }
}
