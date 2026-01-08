package com.example.bankapp.repository;

import com.example.bankapp.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setUsername("testuser");
        testAccount.setPassword("encodedPassword");
        testAccount.setBalance(new BigDecimal("1000.00"));
    }

    @Test
    void findByUsername_WhenAccountExists_ReturnsAccount() {
        entityManager.persistAndFlush(testAccount);

        Optional<Account> found = accountRepository.findByUsername("testuser");

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals(new BigDecimal("1000.00"), found.get().getBalance());
    }

    @Test
    void findByUsername_WhenAccountNotExists_ReturnsEmpty() {
        Optional<Account> found = accountRepository.findByUsername("nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    void save_PersistsNewAccount() {
        Account savedAccount = accountRepository.save(testAccount);

        assertNotNull(savedAccount.getId());
        assertEquals("testuser", savedAccount.getUsername());
    }

    @Test
    void findById_WhenAccountExists_ReturnsAccount() {
        Account persistedAccount = entityManager.persistAndFlush(testAccount);

        Optional<Account> found = accountRepository.findById(persistedAccount.getId());

        assertTrue(found.isPresent());
        assertEquals(persistedAccount.getId(), found.get().getId());
    }

    @Test
    void findById_WhenAccountNotExists_ReturnsEmpty() {
        Optional<Account> found = accountRepository.findById(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void delete_RemovesAccount() {
        Account persistedAccount = entityManager.persistAndFlush(testAccount);
        Long accountId = persistedAccount.getId();

        accountRepository.delete(persistedAccount);
        entityManager.flush();

        Optional<Account> found = accountRepository.findById(accountId);
        assertFalse(found.isPresent());
    }

    @Test
    void update_ModifiesExistingAccount() {
        Account persistedAccount = entityManager.persistAndFlush(testAccount);

        persistedAccount.setBalance(new BigDecimal("2000.00"));
        accountRepository.save(persistedAccount);
        entityManager.flush();

        Optional<Account> found = accountRepository.findById(persistedAccount.getId());
        assertTrue(found.isPresent());
        assertEquals(new BigDecimal("2000.00"), found.get().getBalance());
    }

    @Test
    void findByUsername_IsCaseSensitive() {
        entityManager.persistAndFlush(testAccount);

        Optional<Account> foundLower = accountRepository.findByUsername("testuser");
        Optional<Account> foundUpper = accountRepository.findByUsername("TESTUSER");

        assertTrue(foundLower.isPresent());
        assertFalse(foundUpper.isPresent());
    }

    @Test
    void save_MultipleAccounts_AllPersisted() {
        Account account1 = new Account();
        account1.setUsername("user1");
        account1.setPassword("password1");
        account1.setBalance(new BigDecimal("100.00"));

        Account account2 = new Account();
        account2.setUsername("user2");
        account2.setPassword("password2");
        account2.setBalance(new BigDecimal("200.00"));

        accountRepository.save(account1);
        accountRepository.save(account2);
        entityManager.flush();

        assertEquals(2, accountRepository.count());
    }
}
