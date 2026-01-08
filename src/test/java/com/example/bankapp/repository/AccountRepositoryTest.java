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
    void findByUsername_Success() {
        entityManager.persist(testAccount);
        entityManager.flush();

        Optional<Account> found = accountRepository.findByUsername("testuser");

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals("encodedPassword", found.get().getPassword());
        assertEquals(new BigDecimal("1000.00"), found.get().getBalance());
    }

    @Test
    void findByUsername_NotFound() {
        Optional<Account> found = accountRepository.findByUsername("nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    void findByUsername_MultipleAccounts() {
        Account account1 = new Account();
        account1.setUsername("user1");
        account1.setPassword("password1");
        account1.setBalance(new BigDecimal("500.00"));

        Account account2 = new Account();
        account2.setUsername("user2");
        account2.setPassword("password2");
        account2.setBalance(new BigDecimal("750.00"));

        entityManager.persist(account1);
        entityManager.persist(account2);
        entityManager.flush();

        Optional<Account> foundUser1 = accountRepository.findByUsername("user1");
        Optional<Account> foundUser2 = accountRepository.findByUsername("user2");

        assertTrue(foundUser1.isPresent());
        assertTrue(foundUser2.isPresent());
        assertEquals("user1", foundUser1.get().getUsername());
        assertEquals("user2", foundUser2.get().getUsername());
        assertEquals(new BigDecimal("500.00"), foundUser1.get().getBalance());
        assertEquals(new BigDecimal("750.00"), foundUser2.get().getBalance());
    }

    @Test
    void save_NewAccount() {
        Account savedAccount = accountRepository.save(testAccount);

        assertNotNull(savedAccount.getId());
        assertEquals("testuser", savedAccount.getUsername());
        assertEquals("encodedPassword", savedAccount.getPassword());
        assertEquals(new BigDecimal("1000.00"), savedAccount.getBalance());
    }

    @Test
    void save_UpdateAccount() {
        entityManager.persist(testAccount);
        entityManager.flush();

        testAccount.setBalance(new BigDecimal("1500.00"));
        Account updatedAccount = accountRepository.save(testAccount);

        assertEquals(new BigDecimal("1500.00"), updatedAccount.getBalance());
    }

    @Test
    void findById_Success() {
        entityManager.persist(testAccount);
        entityManager.flush();

        Optional<Account> found = accountRepository.findById(testAccount.getId());

        assertTrue(found.isPresent());
        assertEquals(testAccount.getId(), found.get().getId());
        assertEquals("testuser", found.get().getUsername());
    }

    @Test
    void findById_NotFound() {
        Optional<Account> found = accountRepository.findById(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void delete_Account() {
        entityManager.persist(testAccount);
        entityManager.flush();

        Long accountId = testAccount.getId();
        accountRepository.delete(testAccount);
        entityManager.flush();

        Optional<Account> found = accountRepository.findById(accountId);
        assertFalse(found.isPresent());
    }

    @Test
    void findAll_Empty() {
        assertTrue(accountRepository.findAll().isEmpty());
    }

    @Test
    void findAll_MultipleAccounts() {
        Account account1 = new Account();
        account1.setUsername("user1");
        account1.setPassword("password1");
        account1.setBalance(new BigDecimal("500.00"));

        Account account2 = new Account();
        account2.setUsername("user2");
        account2.setPassword("password2");
        account2.setBalance(new BigDecimal("750.00"));

        entityManager.persist(account1);
        entityManager.persist(account2);
        entityManager.flush();

        assertEquals(2, accountRepository.findAll().size());
    }

    @Test
    void count_Accounts() {
        entityManager.persist(testAccount);
        entityManager.flush();

        assertEquals(1, accountRepository.count());
    }

    @Test
    void existsById_True() {
        entityManager.persist(testAccount);
        entityManager.flush();

        assertTrue(accountRepository.existsById(testAccount.getId()));
    }

    @Test
    void existsById_False() {
        assertFalse(accountRepository.existsById(999L));
    }
}
