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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account testAccount;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setUsername("testuser");
        testAccount.setPassword("password123");
        testAccount.setBalance(new BigDecimal("1000.00"));
        entityManager.persistAndFlush(testAccount);

        testTransaction = new Transaction();
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setType("Deposit");
        testTransaction.setTimestamp(LocalDateTime.now());
        testTransaction.setAccount(testAccount);
        entityManager.persistAndFlush(testTransaction);
    }

    @Test
    void findByAccountId_ExistingAccount_ReturnsTransactions() {
        List<Transaction> transactions = transactionRepository.findByAccountId(testAccount.getId());

        assertFalse(transactions.isEmpty());
        assertEquals(1, transactions.size());
        assertEquals("Deposit", transactions.get(0).getType());
    }

    @Test
    void findByAccountId_NonExistingAccount_ReturnsEmptyList() {
        List<Transaction> transactions = transactionRepository.findByAccountId(999L);

        assertTrue(transactions.isEmpty());
    }

    @Test
    void findByAccountId_MultipleTransactions_ReturnsAll() {
        Transaction t2 = new Transaction();
        t2.setAmount(new BigDecimal("50.00"));
        t2.setType("Withdrawal");
        t2.setTimestamp(LocalDateTime.now());
        t2.setAccount(testAccount);
        entityManager.persistAndFlush(t2);

        Transaction t3 = new Transaction();
        t3.setAmount(new BigDecimal("200.00"));
        t3.setType("Transfer Out to user2");
        t3.setTimestamp(LocalDateTime.now());
        t3.setAccount(testAccount);
        entityManager.persistAndFlush(t3);

        List<Transaction> transactions = transactionRepository.findByAccountId(testAccount.getId());

        assertEquals(3, transactions.size());
    }

    @Test
    void save_NewTransaction_Success() {
        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(new BigDecimal("250.00"));
        newTransaction.setType("Deposit");
        newTransaction.setTimestamp(LocalDateTime.now());
        newTransaction.setAccount(testAccount);

        Transaction saved = transactionRepository.save(newTransaction);

        assertNotNull(saved.getId());
        assertEquals(new BigDecimal("250.00"), saved.getAmount());
    }

    @Test
    void findById_ExistingTransaction_ReturnsTransaction() {
        var found = transactionRepository.findById(testTransaction.getId());

        assertTrue(found.isPresent());
        assertEquals("Deposit", found.get().getType());
    }

    @Test
    void findById_NonExistingTransaction_ReturnsEmpty() {
        var found = transactionRepository.findById(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void deleteTransaction_Success() {
        Long transactionId = testTransaction.getId();
        transactionRepository.delete(testTransaction);
        entityManager.flush();

        var found = transactionRepository.findById(transactionId);
        assertFalse(found.isPresent());
    }

    @Test
    void findAll_ReturnsAllTransactions() {
        Account anotherAccount = new Account();
        anotherAccount.setUsername("anotheruser");
        anotherAccount.setPassword("password");
        anotherAccount.setBalance(BigDecimal.ZERO);
        entityManager.persistAndFlush(anotherAccount);

        Transaction t2 = new Transaction();
        t2.setAmount(new BigDecimal("75.00"));
        t2.setType("Deposit");
        t2.setTimestamp(LocalDateTime.now());
        t2.setAccount(anotherAccount);
        entityManager.persistAndFlush(t2);

        var transactions = transactionRepository.findAll();

        assertEquals(2, transactions.size());
    }

    @Test
    void transactionBelongsToCorrectAccount() {
        List<Transaction> transactions = transactionRepository.findByAccountId(testAccount.getId());

        assertFalse(transactions.isEmpty());
        assertEquals(testAccount.getId(), transactions.get(0).getAccount().getId());
    }
}
