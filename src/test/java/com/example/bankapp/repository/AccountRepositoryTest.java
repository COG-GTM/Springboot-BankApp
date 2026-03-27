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
    void findByUsername_existingUser_returnsAccount() {
        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword("password");
        account.setBalance(new BigDecimal("100.00"));
        accountRepository.save(account);

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
