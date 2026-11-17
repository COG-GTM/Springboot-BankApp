package com.example.bankapp.repository;

import com.example.bankapp.model.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void saveAndFindById() {
        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword("password");
        account.setBalance(new BigDecimal("1000.00"));

        Account saved = accountRepository.save(account);
        assertNotNull(saved.getId());

        Optional<Account> found = accountRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals(new BigDecimal("1000.00"), found.get().getBalance());
    }

    @Test
    void findByUsername_Found() {
        Account account = new Account();
        account.setUsername("john");
        account.setPassword("secret");
        account.setBalance(BigDecimal.ZERO);
        accountRepository.save(account);

        Optional<Account> found = accountRepository.findByUsername("john");
        assertTrue(found.isPresent());
        assertEquals("john", found.get().getUsername());
    }

    @Test
    void findByUsername_NotFound() {
        Optional<Account> found = accountRepository.findByUsername("nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void findAll_ReturnsAllAccounts() {
        Account a1 = new Account();
        a1.setUsername("user1");
        a1.setPassword("pass1");
        a1.setBalance(BigDecimal.TEN);

        Account a2 = new Account();
        a2.setUsername("user2");
        a2.setPassword("pass2");
        a2.setBalance(new BigDecimal("200"));

        accountRepository.save(a1);
        accountRepository.save(a2);

        assertEquals(2, accountRepository.findAll().size());
    }

    @Test
    void deleteAccount() {
        Account account = new Account();
        account.setUsername("todelete");
        account.setPassword("pass");
        account.setBalance(BigDecimal.ZERO);

        Account saved = accountRepository.save(account);
        assertNotNull(saved.getId());

        accountRepository.delete(saved);

        Optional<Account> found = accountRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void updateAccount() {
        Account account = new Account();
        account.setUsername("updatable");
        account.setPassword("pass");
        account.setBalance(new BigDecimal("100"));

        Account saved = accountRepository.save(account);
        saved.setBalance(new BigDecimal("500"));
        accountRepository.save(saved);

        Optional<Account> found = accountRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(new BigDecimal("500"), found.get().getBalance());
    }
}
