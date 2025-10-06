package com.example.bankapp;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class DatabaseConnectivityIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountService accountService;

    @Test
    void testDatabaseConnection() throws SQLException {
        assertNotNull(dataSource, "DataSource should be autowired");
        
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Connection should be established");
            assertFalse(connection.isClosed(), "Connection should be open");
            
            DatabaseMetaData metaData = connection.getMetaData();
            String driverName = metaData.getDriverName();
            String driverVersion = metaData.getDriverVersion();
            
            System.out.println("Database Driver: " + driverName);
            System.out.println("Driver Version: " + driverVersion);
            
            assertTrue(driverName.toLowerCase().contains("mysql"), 
                "Should be using MySQL driver");
        }
    }

    @Test
    void testConnectionPooling() throws SQLException {
        Connection conn1 = dataSource.getConnection();
        Connection conn2 = dataSource.getConnection();
        
        assertNotNull(conn1, "First connection should be established");
        assertNotNull(conn2, "Second connection should be established");
        assertNotSame(conn1, conn2, "Should get different connection instances");
        
        conn1.close();
        conn2.close();
    }

    @Test
    void testAccountRepositoryBasicOperations() {
        Account account = new Account();
        account.setUsername("testuser_" + System.currentTimeMillis());
        account.setPassword("password123");
        account.setBalance(new BigDecimal("1000.00"));
        
        Account savedAccount = accountRepository.save(account);
        
        assertNotNull(savedAccount.getId(), "Saved account should have an ID");
        assertEquals(account.getUsername(), savedAccount.getUsername());
        
        Account foundAccount = accountRepository.findById(savedAccount.getId()).orElse(null);
        assertNotNull(foundAccount, "Should find the saved account");
        assertEquals(savedAccount.getUsername(), foundAccount.getUsername());
    }

    @Test
    void testAccountServiceRegistration() {
        String uniqueUsername = "newuser_" + System.currentTimeMillis();
        
        Account account = accountService.registerAccount(uniqueUsername, "password123");
        
        assertNotNull(account.getId(), "Registered account should have an ID");
        assertEquals(uniqueUsername, account.getUsername());
        assertEquals(new BigDecimal("0"), account.getBalance(), "Initial balance should be 0");
        assertNotNull(account.getPassword(), "Password should be encoded");
        assertNotEquals("password123", account.getPassword(), "Password should be hashed");
    }

    @Test
    void testAccountServiceDeposit() {
        String username = "deposituser_" + System.currentTimeMillis();
        Account account = accountService.registerAccount(username, "password123");
        
        BigDecimal depositAmount = new BigDecimal("500.00");
        accountService.deposit(account, depositAmount);
        
        Account updatedAccount = accountRepository.findById(account.getId()).orElse(null);
        assertNotNull(updatedAccount);
        assertEquals(depositAmount, updatedAccount.getBalance());
        
        List<Transaction> transactions = accountService.getTransactionHistory(updatedAccount);
        assertEquals(1, transactions.size(), "Should have one transaction");
        assertEquals("Deposit", transactions.get(0).getType());
        assertEquals(depositAmount, transactions.get(0).getAmount());
    }

    @Test
    void testAccountServiceWithdrawal() {
        String username = "withdrawuser_" + System.currentTimeMillis();
        Account account = accountService.registerAccount(username, "password123");
        
        accountService.deposit(account, new BigDecimal("1000.00"));
        
        BigDecimal withdrawAmount = new BigDecimal("300.00");
        accountService.withdraw(account, withdrawAmount);
        
        Account updatedAccount = accountRepository.findById(account.getId()).orElse(null);
        assertNotNull(updatedAccount);
        assertEquals(new BigDecimal("700.00"), updatedAccount.getBalance());
        
        List<Transaction> transactions = accountService.getTransactionHistory(updatedAccount);
        assertEquals(2, transactions.size(), "Should have two transactions");
    }

    @Test
    void testAccountServiceWithdrawalInsufficientFunds() {
        String username = "insufficientuser_" + System.currentTimeMillis();
        Account account = accountService.registerAccount(username, "password123");
        
        accountService.deposit(account, new BigDecimal("100.00"));
        
        BigDecimal withdrawAmount = new BigDecimal("200.00");
        
        assertThrows(RuntimeException.class, () -> {
            accountService.withdraw(account, withdrawAmount);
        }, "Should throw exception for insufficient funds");
    }

    @Test
    void testAccountServiceTransfer() {
        String sender = "sender_" + System.currentTimeMillis();
        String recipient = "recipient_" + System.currentTimeMillis();
        
        Account senderAccount = accountService.registerAccount(sender, "password123");
        Account recipientAccount = accountService.registerAccount(recipient, "password123");
        
        accountService.deposit(senderAccount, new BigDecimal("1000.00"));
        
        BigDecimal transferAmount = new BigDecimal("250.00");
        accountService.transferAmount(senderAccount, recipient, transferAmount);
        
        Account updatedSender = accountRepository.findById(senderAccount.getId()).orElse(null);
        Account updatedRecipient = accountRepository.findById(recipientAccount.getId()).orElse(null);
        
        assertNotNull(updatedSender);
        assertNotNull(updatedRecipient);
        assertEquals(new BigDecimal("750.00"), updatedSender.getBalance());
        assertEquals(new BigDecimal("250.00"), updatedRecipient.getBalance());
        
        List<Transaction> senderTransactions = accountService.getTransactionHistory(updatedSender);
        List<Transaction> recipientTransactions = accountService.getTransactionHistory(updatedRecipient);
        
        assertTrue(senderTransactions.size() >= 2, "Sender should have deposit and transfer out");
        assertTrue(recipientTransactions.size() >= 1, "Recipient should have transfer in");
    }

    @Test
    void testTransactionRepositoryQuery() {
        String username = "txnuser_" + System.currentTimeMillis();
        Account account = accountService.registerAccount(username, "password123");
        
        accountService.deposit(account, new BigDecimal("100.00"));
        accountService.deposit(account, new BigDecimal("200.00"));
        accountService.withdraw(account, new BigDecimal("50.00"));
        
        List<Transaction> transactions = transactionRepository.findByAccountId(account.getId());
        
        assertEquals(3, transactions.size(), "Should have three transactions");
        assertTrue(transactions.stream().anyMatch(t -> t.getType().equals("Deposit")));
        assertTrue(transactions.stream().anyMatch(t -> t.getType().equals("Withdrawal")));
    }

    @Test
    void testAccountRepositoryFindByUsername() {
        String username = "findbyusername_" + System.currentTimeMillis();
        Account account = accountService.registerAccount(username, "password123");
        
        Account foundAccount = accountRepository.findByUsername(username).orElse(null);
        
        assertNotNull(foundAccount, "Should find account by username");
        assertEquals(account.getId(), foundAccount.getId());
        assertEquals(username, foundAccount.getUsername());
    }

    @Test
    void testJpaRelationship() {
        String username = "relationshipuser_" + System.currentTimeMillis();
        Account account = accountService.registerAccount(username, "password123");
        
        accountService.deposit(account, new BigDecimal("500.00"));
        
        Account foundAccount = accountRepository.findById(account.getId()).orElse(null);
        assertNotNull(foundAccount);
        
        List<Transaction> transactions = transactionRepository.findByAccountId(foundAccount.getId());
        assertFalse(transactions.isEmpty(), "Should have transactions");
        
        Transaction transaction = transactions.get(0);
        assertNotNull(transaction.getAccount(), "Transaction should have account reference");
        assertEquals(foundAccount.getId(), transaction.getAccount().getId());
    }
}
