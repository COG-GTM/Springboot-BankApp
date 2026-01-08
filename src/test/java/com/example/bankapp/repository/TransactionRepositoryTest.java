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
        testAccount.setPassword("encodedPassword");
        testAccount.setBalance(new BigDecimal("1000.00"));
        entityManager.persistAndFlush(testAccount);

        testTransaction = new Transaction();
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setType("Deposit");
        testTransaction.setTimestamp(LocalDateTime.now());
        testTransaction.setAccount(testAccount);
    }

    @Test
    void findByAccountId_WhenTransactionsExist_ReturnsTransactions() {
        entityManager.persistAndFlush(testTransaction);

        List<Transaction> found = transactionRepository.findByAccountId(testAccount.getId());

        assertFalse(found.isEmpty());
        assertEquals(1, found.size());
        assertEquals("Deposit", found.get(0).getType());
    }

    @Test
    void findByAccountId_WhenNoTransactions_ReturnsEmptyList() {
        List<Transaction> found = transactionRepository.findByAccountId(testAccount.getId());

        assertTrue(found.isEmpty());
    }

    @Test
    void findByAccountId_WhenAccountNotExists_ReturnsEmptyList() {
        List<Transaction> found = transactionRepository.findByAccountId(999L);

        assertTrue(found.isEmpty());
    }

    @Test
    void save_PersistsNewTransaction() {
        Transaction savedTransaction = transactionRepository.save(testTransaction);

        assertNotNull(savedTransaction.getId());
        assertEquals("Deposit", savedTransaction.getType());
        assertEquals(new BigDecimal("100.00"), savedTransaction.getAmount());
    }

    @Test
    void findByAccountId_MultipleTransactions_ReturnsAll() {
        Transaction transaction1 = new Transaction();
        transaction1.setAmount(new BigDecimal("100.00"));
        transaction1.setType("Deposit");
        transaction1.setTimestamp(LocalDateTime.now());
        transaction1.setAccount(testAccount);

        Transaction transaction2 = new Transaction();
        transaction2.setAmount(new BigDecimal("50.00"));
        transaction2.setType("Withdrawal");
        transaction2.setTimestamp(LocalDateTime.now());
        transaction2.setAccount(testAccount);

        entityManager.persistAndFlush(transaction1);
        entityManager.persistAndFlush(transaction2);

        List<Transaction> found = transactionRepository.findByAccountId(testAccount.getId());

        assertEquals(2, found.size());
    }

    @Test
    void findByAccountId_OnlyReturnsTransactionsForSpecificAccount() {
        Account anotherAccount = new Account();
        anotherAccount.setUsername("anotheruser");
        anotherAccount.setPassword("password");
        anotherAccount.setBalance(new BigDecimal("500.00"));
        entityManager.persistAndFlush(anotherAccount);

        Transaction transactionForTestAccount = new Transaction();
        transactionForTestAccount.setAmount(new BigDecimal("100.00"));
        transactionForTestAccount.setType("Deposit");
        transactionForTestAccount.setTimestamp(LocalDateTime.now());
        transactionForTestAccount.setAccount(testAccount);

        Transaction transactionForAnotherAccount = new Transaction();
        transactionForAnotherAccount.setAmount(new BigDecimal("200.00"));
        transactionForAnotherAccount.setType("Deposit");
        transactionForAnotherAccount.setTimestamp(LocalDateTime.now());
        transactionForAnotherAccount.setAccount(anotherAccount);

        entityManager.persistAndFlush(transactionForTestAccount);
        entityManager.persistAndFlush(transactionForAnotherAccount);

        List<Transaction> foundForTestAccount = transactionRepository.findByAccountId(testAccount.getId());
        List<Transaction> foundForAnotherAccount = transactionRepository.findByAccountId(anotherAccount.getId());

        assertEquals(1, foundForTestAccount.size());
        assertEquals(1, foundForAnotherAccount.size());
        assertEquals(new BigDecimal("100.00"), foundForTestAccount.get(0).getAmount());
        assertEquals(new BigDecimal("200.00"), foundForAnotherAccount.get(0).getAmount());
    }

    @Test
    void delete_RemovesTransaction() {
        Transaction persistedTransaction = entityManager.persistAndFlush(testTransaction);
        Long transactionId = persistedTransaction.getId();

        transactionRepository.delete(persistedTransaction);
        entityManager.flush();

        assertFalse(transactionRepository.findById(transactionId).isPresent());
    }

    @Test
    void count_ReturnsCorrectCount() {
        entityManager.persistAndFlush(testTransaction);

        Transaction anotherTransaction = new Transaction();
        anotherTransaction.setAmount(new BigDecimal("200.00"));
        anotherTransaction.setType("Withdrawal");
        anotherTransaction.setTimestamp(LocalDateTime.now());
        anotherTransaction.setAccount(testAccount);
        entityManager.persistAndFlush(anotherTransaction);

        assertEquals(2, transactionRepository.count());
    }
}
