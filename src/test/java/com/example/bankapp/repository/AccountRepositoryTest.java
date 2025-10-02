package com.example.bankapp.repository;

import com.example.bankapp.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Optional;

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
    void testFindByUsername_Success() {
        entityManager.persistAndFlush(testAccount);

        Optional<Account> result = accountRepository.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals(new BigDecimal("1000.00"), result.get().getBalance());
    }

    @Test
    void testFindByUsername_NotFound() {
        Optional<Account> result = accountRepository.findByUsername("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void testSaveAccount() {
        Account savedAccount = accountRepository.save(testAccount);

        assertNotNull(savedAccount.getId());
        assertEquals("testuser", savedAccount.getUsername());
        assertEquals(new BigDecimal("1000.00"), savedAccount.getBalance());
    }

    @Test
    void testFindByUsername_CaseSensitive() {
        entityManager.persistAndFlush(testAccount);

        Optional<Account> result = accountRepository.findByUsername("TESTUSER");

        assertFalse(result.isPresent());
    }

    @Test
    void testUpdateAccountBalance() {
        Account savedAccount = entityManager.persistAndFlush(testAccount);
        savedAccount.setBalance(new BigDecimal("1500.00"));

        Account updatedAccount = accountRepository.save(savedAccount);

        assertEquals(new BigDecimal("1500.00"), updatedAccount.getBalance());
    }
}
