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
        testAccount.setPassword("password123");
        testAccount.setBalance(new BigDecimal("1000.00"));
        entityManager.persistAndFlush(testAccount);
    }

    @Test
    void findByUsername_ExistingUser_ReturnsAccount() {
        Optional<Account> found = accountRepository.findByUsername("testuser");

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals(new BigDecimal("1000.00"), found.get().getBalance());
    }

    @Test
    void findByUsername_NonExistingUser_ReturnsEmpty() {
        Optional<Account> found = accountRepository.findByUsername("nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    void save_NewAccount_Success() {
        Account newAccount = new Account();
        newAccount.setUsername("newuser");
        newAccount.setPassword("newpassword");
        newAccount.setBalance(new BigDecimal("500.00"));

        Account saved = accountRepository.save(newAccount);

        assertNotNull(saved.getId());
        assertEquals("newuser", saved.getUsername());
    }

    @Test
    void findById_ExistingAccount_ReturnsAccount() {
        Optional<Account> found = accountRepository.findById(testAccount.getId());

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }

    @Test
    void findById_NonExistingAccount_ReturnsEmpty() {
        Optional<Account> found = accountRepository.findById(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void updateBalance_Success() {
        testAccount.setBalance(new BigDecimal("1500.00"));
        Account updated = accountRepository.save(testAccount);

        assertEquals(new BigDecimal("1500.00"), updated.getBalance());
    }

    @Test
    void deleteAccount_Success() {
        Long accountId = testAccount.getId();
        accountRepository.delete(testAccount);
        entityManager.flush();

        Optional<Account> found = accountRepository.findById(accountId);
        assertFalse(found.isPresent());
    }

    @Test
    void findAll_ReturnsAllAccounts() {
        Account anotherAccount = new Account();
        anotherAccount.setUsername("anotheruser");
        anotherAccount.setPassword("password");
        anotherAccount.setBalance(BigDecimal.ZERO);
        entityManager.persistAndFlush(anotherAccount);

        var accounts = accountRepository.findAll();

        assertEquals(2, accounts.size());
    }
}
