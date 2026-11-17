package com.example.bankapp.repository;

import com.example.bankapp.model.Account;
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

    @Test
    void findByUsername_existingUser_returnsAccount() {
        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword("password");
        account.setBalance(new BigDecimal("100.00"));
        entityManager.persistAndFlush(account);

        Optional<Account> found = accountRepository.findByUsername("testuser");

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals(new BigDecimal("100.00"), found.get().getBalance());
    }

    @Test
    void findByUsername_nonExistentUser_returnsEmpty() {
        Optional<Account> found = accountRepository.findByUsername("nonexistent");

        assertFalse(found.isPresent());
    }
}
