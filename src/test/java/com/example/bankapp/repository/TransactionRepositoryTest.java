package com.example.bankapp.repository;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
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
        testAccount.setPassword("encodedPassword");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount = entityManager.persistAndFlush(testAccount);
    }

    @Test
    void testFindByAccountId_MultipleTransactions() {
        Transaction transaction1 = new Transaction(
            new BigDecimal("100.00"), 
            "Deposit", 
            LocalDateTime.now(), 
            testAccount
        );
        Transaction transaction2 = new Transaction(
            new BigDecimal("50.00"), 
            "Withdrawal", 
            LocalDateTime.now(), 
            testAccount
        );
        entityManager.persist(transaction1);
        entityManager.persist(transaction2);
        entityManager.flush();

        List<Transaction> transactions = transactionRepository.findByAccountId(testAccount.getId());

        assertEquals(2, transactions.size());
    }

    @Test
    void testFindByAccountId_NoTransactions() {
        List<Transaction> transactions = transactionRepository.findByAccountId(testAccount.getId());

        assertTrue(transactions.isEmpty());
    }

    @Test
    void testSaveTransaction() {
        Transaction transaction = new Transaction(
            new BigDecimal("200.00"), 
            "Deposit", 
            LocalDateTime.now(), 
            testAccount
        );

        Transaction savedTransaction = transactionRepository.save(transaction);

        assertNotNull(savedTransaction.getId());
        assertEquals(new BigDecimal("200.00"), savedTransaction.getAmount());
        assertEquals("Deposit", savedTransaction.getType());
        assertEquals(testAccount.getId(), savedTransaction.getAccount().getId());
    }

    @Test
    void testFindByAccountId_OrderedByTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        Transaction older = new Transaction(new BigDecimal("100.00"), "Deposit", now.minusHours(1), testAccount);
        Transaction newer = new Transaction(new BigDecimal("50.00"), "Withdrawal", now, testAccount);
        entityManager.persist(older);
        entityManager.persist(newer);
        entityManager.flush();

        List<Transaction> transactions = transactionRepository.findByAccountId(testAccount.getId());

        assertEquals(2, transactions.size());
    }
}
