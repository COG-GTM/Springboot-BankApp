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
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setUsername("testuser");
        testAccount.setPassword("encodedPassword");
        testAccount.setBalance(new BigDecimal("1000.00"));
        entityManager.persist(testAccount);

        testTransaction = new Transaction();
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setType("Deposit");
        testTransaction.setTimestamp(LocalDateTime.now());
        testTransaction.setAccount(testAccount);
    }

    @Test
    void findByAccountId_Success() {
        entityManager.persist(testTransaction);
        entityManager.flush();

        List<Transaction> transactions = transactionRepository.findByAccountId(testAccount.getId());

        assertFalse(transactions.isEmpty());
        assertEquals(1, transactions.size());
        assertEquals(new BigDecimal("100.00"), transactions.get(0).getAmount());
        assertEquals("Deposit", transactions.get(0).getType());
    }

    @Test
    void findByAccountId_MultipleTransactions() {
        Transaction t1 = new Transaction(new BigDecimal("100.00"), "Deposit", LocalDateTime.now(), testAccount);
        Transaction t2 = new Transaction(new BigDecimal("50.00"), "Withdrawal", LocalDateTime.now(), testAccount);
        Transaction t3 = new Transaction(new BigDecimal("200.00"), "Transfer In from sender", LocalDateTime.now(), testAccount);

        entityManager.persist(t1);
        entityManager.persist(t2);
        entityManager.persist(t3);
        entityManager.flush();

        List<Transaction> transactions = transactionRepository.findByAccountId(testAccount.getId());

        assertEquals(3, transactions.size());
    }

    @Test
    void findByAccountId_NoTransactions() {
        List<Transaction> transactions = transactionRepository.findByAccountId(testAccount.getId());

        assertTrue(transactions.isEmpty());
    }

    @Test
    void findByAccountId_DifferentAccounts() {
        Account account2 = new Account();
        account2.setUsername("user2");
        account2.setPassword("password2");
        account2.setBalance(new BigDecimal("500.00"));
        entityManager.persist(account2);

        Transaction t1 = new Transaction(new BigDecimal("100.00"), "Deposit", LocalDateTime.now(), testAccount);
        Transaction t2 = new Transaction(new BigDecimal("200.00"), "Deposit", LocalDateTime.now(), account2);

        entityManager.persist(t1);
        entityManager.persist(t2);
        entityManager.flush();

        List<Transaction> transactionsAccount1 = transactionRepository.findByAccountId(testAccount.getId());
        List<Transaction> transactionsAccount2 = transactionRepository.findByAccountId(account2.getId());

        assertEquals(1, transactionsAccount1.size());
        assertEquals(1, transactionsAccount2.size());
        assertEquals(new BigDecimal("100.00"), transactionsAccount1.get(0).getAmount());
        assertEquals(new BigDecimal("200.00"), transactionsAccount2.get(0).getAmount());
    }

    @Test
    void findByAccountId_NonExistentAccount() {
        List<Transaction> transactions = transactionRepository.findByAccountId(999L);

        assertTrue(transactions.isEmpty());
    }

    @Test
    void save_NewTransaction() {
        Transaction savedTransaction = transactionRepository.save(testTransaction);

        assertNotNull(savedTransaction.getId());
        assertEquals(new BigDecimal("100.00"), savedTransaction.getAmount());
        assertEquals("Deposit", savedTransaction.getType());
        assertEquals(testAccount, savedTransaction.getAccount());
    }

    @Test
    void save_UpdateTransaction() {
        entityManager.persist(testTransaction);
        entityManager.flush();

        testTransaction.setAmount(new BigDecimal("150.00"));
        Transaction updatedTransaction = transactionRepository.save(testTransaction);

        assertEquals(new BigDecimal("150.00"), updatedTransaction.getAmount());
    }

    @Test
    void findById_Success() {
        entityManager.persist(testTransaction);
        entityManager.flush();

        Optional<Transaction> found = transactionRepository.findById(testTransaction.getId());

        assertTrue(found.isPresent());
        assertEquals(testTransaction.getId(), found.get().getId());
        assertEquals("Deposit", found.get().getType());
    }

    @Test
    void findById_NotFound() {
        Optional<Transaction> found = transactionRepository.findById(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void delete_Transaction() {
        entityManager.persist(testTransaction);
        entityManager.flush();

        Long transactionId = testTransaction.getId();
        transactionRepository.delete(testTransaction);
        entityManager.flush();

        Optional<Transaction> found = transactionRepository.findById(transactionId);
        assertFalse(found.isPresent());
    }

    @Test
    void findAll_Empty() {
        assertTrue(transactionRepository.findAll().isEmpty());
    }

    @Test
    void findAll_MultipleTransactions() {
        Transaction t1 = new Transaction(new BigDecimal("100.00"), "Deposit", LocalDateTime.now(), testAccount);
        Transaction t2 = new Transaction(new BigDecimal("50.00"), "Withdrawal", LocalDateTime.now(), testAccount);

        entityManager.persist(t1);
        entityManager.persist(t2);
        entityManager.flush();

        assertEquals(2, transactionRepository.findAll().size());
    }

    @Test
    void count_Transactions() {
        entityManager.persist(testTransaction);
        entityManager.flush();

        assertEquals(1, transactionRepository.count());
    }

    @Test
    void existsById_True() {
        entityManager.persist(testTransaction);
        entityManager.flush();

        assertTrue(transactionRepository.existsById(testTransaction.getId()));
    }

    @Test
    void existsById_False() {
        assertFalse(transactionRepository.existsById(999L));
    }

    @Test
    void saveDepositTransaction() {
        Transaction deposit = new Transaction(
            new BigDecimal("500.00"),
            "Deposit",
            LocalDateTime.now(),
            testAccount
        );

        Transaction saved = transactionRepository.save(deposit);

        assertNotNull(saved.getId());
        assertEquals("Deposit", saved.getType());
        assertEquals(new BigDecimal("500.00"), saved.getAmount());
    }

    @Test
    void saveWithdrawalTransaction() {
        Transaction withdrawal = new Transaction(
            new BigDecimal("200.00"),
            "Withdrawal",
            LocalDateTime.now(),
            testAccount
        );

        Transaction saved = transactionRepository.save(withdrawal);

        assertNotNull(saved.getId());
        assertEquals("Withdrawal", saved.getType());
        assertEquals(new BigDecimal("200.00"), saved.getAmount());
    }

    @Test
    void saveTransferTransaction() {
        Transaction transfer = new Transaction(
            new BigDecimal("300.00"),
            "Transfer Out to recipient",
            LocalDateTime.now(),
            testAccount
        );

        Transaction saved = transactionRepository.save(transfer);

        assertNotNull(saved.getId());
        assertTrue(saved.getType().contains("Transfer"));
        assertEquals(new BigDecimal("300.00"), saved.getAmount());
    }
}
