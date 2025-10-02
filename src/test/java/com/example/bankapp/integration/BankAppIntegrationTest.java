package com.example.bankapp.integration;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class BankAppIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void testCompleteUserRegistrationAndLogin() {
        Account newAccount = accountService.registerAccount("integrationuser", "password123");

        assertNotNull(newAccount);
        assertNotNull(newAccount.getId());
        assertEquals("integrationuser", newAccount.getUsername());
        assertEquals(BigDecimal.ZERO, newAccount.getBalance());

        Account foundAccount = accountService.findAccountByUsername("integrationuser");
        assertEquals(newAccount.getId(), foundAccount.getId());
        assertEquals("integrationuser", foundAccount.getUsername());
    }

    @Test
    void testDepositWorkflow() {
        Account account = accountService.registerAccount("deposituser", "password123");
        
        accountService.deposit(account, new BigDecimal("500.00"));

        Account updatedAccount = accountService.findAccountByUsername("deposituser");
        assertEquals(new BigDecimal("500.00"), updatedAccount.getBalance());
        assertEquals(1, accountService.getTransactionHistory(updatedAccount).size());
    }

    @Test
    void testWithdrawalWorkflow() {
        Account account = accountService.registerAccount("withdrawuser", "password123");
        accountService.deposit(account, new BigDecimal("1000.00"));
        
        accountService.withdraw(account, new BigDecimal("300.00"));

        Account updatedAccount = accountService.findAccountByUsername("withdrawuser");
        assertEquals(new BigDecimal("700.00"), updatedAccount.getBalance());
        assertEquals(2, accountService.getTransactionHistory(updatedAccount).size());
    }

    @Test
    void testTransferWorkflow() {
        Account fromAccount = accountService.registerAccount("sender", "password123");
        Account toAccount = accountService.registerAccount("receiver", "password123");
        accountService.deposit(fromAccount, new BigDecimal("1000.00"));
        
        accountService.transferAmount(fromAccount, "receiver", new BigDecimal("400.00"));

        Account updatedSender = accountService.findAccountByUsername("sender");
        Account updatedReceiver = accountService.findAccountByUsername("receiver");
        
        assertEquals(new BigDecimal("600.00"), updatedSender.getBalance());
        assertEquals(new BigDecimal("400.00"), updatedReceiver.getBalance());
        assertEquals(2, accountService.getTransactionHistory(updatedSender).size());
        assertEquals(1, accountService.getTransactionHistory(updatedReceiver).size());
    }

    @Test
    void testMultipleTransactionsWorkflow() {
        Account account = accountService.registerAccount("multiuser", "password123");
        
        accountService.deposit(account, new BigDecimal("1000.00"));
        accountService.withdraw(account, new BigDecimal("200.00"));
        accountService.deposit(account, new BigDecimal("500.00"));
        accountService.withdraw(account, new BigDecimal("100.00"));

        Account finalAccount = accountService.findAccountByUsername("multiuser");
        assertEquals(new BigDecimal("1200.00"), finalAccount.getBalance());
        assertEquals(4, accountService.getTransactionHistory(finalAccount).size());
    }

    @Test
    void testInsufficientFundsWorkflow() {
        Account account = accountService.registerAccount("pooruser", "password123");
        accountService.deposit(account, new BigDecimal("100.00"));

        assertThrows(RuntimeException.class, () -> 
            accountService.withdraw(account, new BigDecimal("200.00"))
        );

        Account unchangedAccount = accountService.findAccountByUsername("pooruser");
        assertEquals(new BigDecimal("100.00"), unchangedAccount.getBalance());
    }

    @Test
    void testTransferToNonexistentAccountWorkflow() {
        Account account = accountService.registerAccount("transferuser", "password123");
        accountService.deposit(account, new BigDecimal("1000.00"));

        assertThrows(RuntimeException.class, () -> 
            accountService.transferAmount(account, "nonexistent", new BigDecimal("100.00"))
        );

        Account unchangedAccount = accountService.findAccountByUsername("transferuser");
        assertEquals(new BigDecimal("1000.00"), unchangedAccount.getBalance());
    }
}
