package com.example.bankapp.repository;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setUsername("testuser");
        testAccount.setPassword("password");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount = entityManager.persistAndFlush(testAccount);
    }

    @Test
    void findByAccountId_returnsTransactions() {
        Transaction t1 = new Transaction(new BigDecimal("100.00"), "Deposit", LocalDateTime.now(), testAccount);
        Transaction t2 = new Transaction(new BigDecimal("50.00"), "Withdrawal", LocalDateTime.now(), testAccount);
        entityManager.persistAndFlush(t1);
        entityManager.persistAndFlush(t2);

        List<Transaction> result = transactionRepository.findByAccountId(testAccount.getId());

        assertEquals(2, result.size());
    }

    @Test
    void findByAccountId_noTransactions() {
        List<Transaction> result = transactionRepository.findByAccountId(testAccount.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findByAccountId_onlyReturnsOwnTransactions() {
        Account otherAccount = new Account();
        otherAccount.setUsername("otheruser");
        otherAccount.setPassword("password");
        otherAccount.setBalance(new BigDecimal("500.00"));
        otherAccount = entityManager.persistAndFlush(otherAccount);

        Transaction t1 = new Transaction(new BigDecimal("100.00"), "Deposit", LocalDateTime.now(), testAccount);
        Transaction t2 = new Transaction(new BigDecimal("200.00"), "Deposit", LocalDateTime.now(), otherAccount);
        entityManager.persistAndFlush(t1);
        entityManager.persistAndFlush(t2);

        List<Transaction> result = transactionRepository.findByAccountId(testAccount.getId());

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("100.00"), result.get(0).getAmount());
    }

    @Test
    void save_createsTransaction() {
        Transaction transaction = new Transaction(
                new BigDecimal("250.00"), "Deposit", LocalDateTime.now(), testAccount);

        Transaction saved = transactionRepository.save(transaction);

        assertNotNull(saved.getId());
        assertEquals(new BigDecimal("250.00"), saved.getAmount());
        assertEquals("Deposit", saved.getType());
    }

    @Test
    void findById_found() {
        Transaction transaction = new Transaction(
                new BigDecimal("300.00"), "Withdrawal", LocalDateTime.now(), testAccount);
        Transaction persisted = entityManager.persistAndFlush(transaction);

        Optional<Transaction> result = transactionRepository.findById(persisted.getId());

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("300.00"), result.get().getAmount());
    }

    @Test
    void findById_notFound() {
        Optional<Transaction> result = transactionRepository.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void findAll_returnsAllTransactions() {
        Transaction t1 = new Transaction(new BigDecimal("100.00"), "Deposit", LocalDateTime.now(), testAccount);
        Transaction t2 = new Transaction(new BigDecimal("50.00"), "Withdrawal", LocalDateTime.now(), testAccount);
        entityManager.persistAndFlush(t1);
        entityManager.persistAndFlush(t2);

        List<Transaction> result = transactionRepository.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void delete_removesTransaction() {
        Transaction transaction = new Transaction(
                new BigDecimal("100.00"), "Deposit", LocalDateTime.now(), testAccount);
        Transaction persisted = entityManager.persistAndFlush(transaction);

        transactionRepository.deleteById(persisted.getId());
        entityManager.flush();

        Optional<Transaction> result = transactionRepository.findById(persisted.getId());
        assertFalse(result.isPresent());
    }

    @Test
    void findByAccountId_nonExistentAccountId() {
        List<Transaction> result = transactionRepository.findByAccountId(999L);

        assertTrue(result.isEmpty());
    }
}
