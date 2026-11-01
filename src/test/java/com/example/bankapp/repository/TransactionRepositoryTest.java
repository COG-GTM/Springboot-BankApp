package com.example.bankapp.repository;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Account createAndSaveAccount(String username) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("password");
        account.setBalance(new BigDecimal("1000.00"));
        return accountRepository.save(account);
    }

    @Test
    void saveAndFindById() {
        Account account = createAndSaveAccount("testuser");

        Transaction transaction = new Transaction(
                new BigDecimal("100.00"), "Deposit", LocalDateTime.now(), account);
        Transaction saved = transactionRepository.save(transaction);

        assertNotNull(saved.getId());
        Optional<Transaction> found = transactionRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Deposit", found.get().getType());
        assertEquals(new BigDecimal("100.00"), found.get().getAmount());
    }

    @Test
    void findByAccountId_ReturnsTransactions() {
        Account account = createAndSaveAccount("user1");

        Transaction t1 = new Transaction(new BigDecimal("100"), "Deposit", LocalDateTime.now(), account);
        Transaction t2 = new Transaction(new BigDecimal("50"), "Withdrawal", LocalDateTime.now(), account);
        transactionRepository.save(t1);
        transactionRepository.save(t2);

        List<Transaction> transactions = transactionRepository.findByAccountId(account.getId());
        assertEquals(2, transactions.size());
    }

    @Test
    void findByAccountId_NoTransactions() {
        Account account = createAndSaveAccount("user2");

        List<Transaction> transactions = transactionRepository.findByAccountId(account.getId());
        assertTrue(transactions.isEmpty());
    }

    @Test
    void findByAccountId_OnlyReturnsOwnTransactions() {
        Account account1 = createAndSaveAccount("userA");
        Account account2 = createAndSaveAccount("userB");

        transactionRepository.save(new Transaction(new BigDecimal("100"), "Deposit", LocalDateTime.now(), account1));
        transactionRepository.save(new Transaction(new BigDecimal("200"), "Deposit", LocalDateTime.now(), account1));
        transactionRepository.save(new Transaction(new BigDecimal("300"), "Deposit", LocalDateTime.now(), account2));

        List<Transaction> txForAccount1 = transactionRepository.findByAccountId(account1.getId());
        assertEquals(2, txForAccount1.size());

        List<Transaction> txForAccount2 = transactionRepository.findByAccountId(account2.getId());
        assertEquals(1, txForAccount2.size());
    }

    @Test
    void findAll_ReturnsAllTransactions() {
        Account account = createAndSaveAccount("user3");

        transactionRepository.save(new Transaction(new BigDecimal("10"), "Deposit", LocalDateTime.now(), account));
        transactionRepository.save(new Transaction(new BigDecimal("20"), "Withdrawal", LocalDateTime.now(), account));
        transactionRepository.save(new Transaction(new BigDecimal("30"), "Transfer Out to x", LocalDateTime.now(), account));

        assertEquals(3, transactionRepository.findAll().size());
    }

    @Test
    void deleteTransaction() {
        Account account = createAndSaveAccount("user4");

        Transaction transaction = new Transaction(new BigDecimal("50"), "Deposit", LocalDateTime.now(), account);
        Transaction saved = transactionRepository.save(transaction);

        transactionRepository.delete(saved);

        Optional<Transaction> found = transactionRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }
}
