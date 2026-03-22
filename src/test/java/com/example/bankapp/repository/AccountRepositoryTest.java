package com.example.bankapp.repository;

import com.example.bankapp.model.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void findByUsername_found() {
        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword("password");
        account.setBalance(new BigDecimal("500.00"));
        entityManager.persistAndFlush(account);

        Optional<Account> result = accountRepository.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals(new BigDecimal("500.00"), result.get().getBalance());
    }

    @Test
    void findByUsername_notFound() {
        Optional<Account> result = accountRepository.findByUsername("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void save_createsAccount() {
        Account account = new Account();
        account.setUsername("newuser");
        account.setPassword("password123");
        account.setBalance(BigDecimal.ZERO);

        Account saved = accountRepository.save(account);

        assertNotNull(saved.getId());
        assertEquals("newuser", saved.getUsername());
    }

    @Test
    void findById_found() {
        Account account = new Account();
        account.setUsername("user1");
        account.setPassword("pass");
        account.setBalance(new BigDecimal("100.00"));
        Account persisted = entityManager.persistAndFlush(account);

        Optional<Account> result = accountRepository.findById(persisted.getId());

        assertTrue(result.isPresent());
        assertEquals("user1", result.get().getUsername());
    }

    @Test
    void findById_notFound() {
        Optional<Account> result = accountRepository.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void findAll_returnsAllAccounts() {
        Account account1 = new Account();
        account1.setUsername("user1");
        account1.setPassword("pass1");
        account1.setBalance(new BigDecimal("100.00"));
        entityManager.persistAndFlush(account1);

        Account account2 = new Account();
        account2.setUsername("user2");
        account2.setPassword("pass2");
        account2.setBalance(new BigDecimal("200.00"));
        entityManager.persistAndFlush(account2);

        List<Account> accounts = accountRepository.findAll();

        assertEquals(2, accounts.size());
    }

    @Test
    void delete_removesAccount() {
        Account account = new Account();
        account.setUsername("todelete");
        account.setPassword("pass");
        account.setBalance(BigDecimal.ZERO);
        Account persisted = entityManager.persistAndFlush(account);

        accountRepository.deleteById(persisted.getId());
        entityManager.flush();

        Optional<Account> result = accountRepository.findById(persisted.getId());
        assertFalse(result.isPresent());
    }

    @Test
    void save_updatesAccount() {
        Account account = new Account();
        account.setUsername("user1");
        account.setPassword("pass");
        account.setBalance(new BigDecimal("100.00"));
        Account persisted = entityManager.persistAndFlush(account);

        persisted.setBalance(new BigDecimal("200.00"));
        accountRepository.save(persisted);
        entityManager.flush();
        entityManager.clear();

        Optional<Account> result = accountRepository.findById(persisted.getId());
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("200.00"), result.get().getBalance());
    }

    @Test
    void findByUsername_multipleAccounts_returnsCorrectOne() {
        Account account1 = new Account();
        account1.setUsername("alice");
        account1.setPassword("pass1");
        account1.setBalance(new BigDecimal("100.00"));
        entityManager.persistAndFlush(account1);

        Account account2 = new Account();
        account2.setUsername("bob");
        account2.setPassword("pass2");
        account2.setBalance(new BigDecimal("200.00"));
        entityManager.persistAndFlush(account2);

        Optional<Account> result = accountRepository.findByUsername("bob");

        assertTrue(result.isPresent());
        assertEquals("bob", result.get().getUsername());
        assertEquals(new BigDecimal("200.00"), result.get().getBalance());
    }
}
