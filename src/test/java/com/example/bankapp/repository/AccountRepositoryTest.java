package com.example.bankapp.repository;

import com.example.bankapp.model.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void findByUsername_found() {
        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword("password");
        account.setBalance(BigDecimal.TEN);
        accountRepository.save(account);

        Optional<Account> result = accountRepository.findByUsername("testuser");
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void findByUsername_notFound() {
        Optional<Account> result = accountRepository.findByUsername("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    void saveAndFindById() {
        Account account = new Account();
        account.setUsername("user1");
        account.setPassword("pass");
        account.setBalance(BigDecimal.ZERO);
        Account saved = accountRepository.save(account);

        assertNotNull(saved.getId());
        Optional<Account> found = accountRepository.findById(saved.getId());
        assertTrue(found.isPresent());
    }
}
