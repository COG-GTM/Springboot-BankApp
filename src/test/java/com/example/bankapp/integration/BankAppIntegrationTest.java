package com.example.bankapp.integration;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BankAppIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    // --- Registration integration tests ---

    @Test
    void registerAccount_shouldPersistToDatabase() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "integrationuser")
                        .param("password", "securePass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertTrue(accountRepository.findByUsername("integrationuser").isPresent());
        Account saved = accountRepository.findByUsername("integrationuser").get();
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getBalance()));
        assertNotNull(saved.getPassword());
        assertNotEquals("securePass123", saved.getPassword()); // should be encoded
    }

    @Test
    void registerAccount_shouldRejectDuplicateUsername() throws Exception {
        // Register first time
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "dupuser")
                        .param("password", "pass1"))
                .andExpect(status().is3xxRedirection());

        // Register same username again
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "dupuser")
                        .param("password", "pass2"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    // --- Full banking flow integration test ---

    @Test
    @WithMockUser(username = "bankuser")
    void fullBankingFlow_registerDepositWithdraw() throws Exception {
        // Register account directly via service (to set up for authenticated tests)
        Account account = accountService.registerAccount("bankuser", "password");

        // Deposit
        mockMvc.perform(post("/deposit")
                        .with(csrf())
                        .param("amount", "1000.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        // Verify balance after deposit
        Account afterDeposit = accountRepository.findByUsername("bankuser").get();
        assertEquals(0, new BigDecimal("1000.00").compareTo(afterDeposit.getBalance()));

        // Withdraw
        mockMvc.perform(post("/withdraw")
                        .with(csrf())
                        .param("amount", "300.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        // Verify balance after withdrawal
        Account afterWithdraw = accountRepository.findByUsername("bankuser").get();
        assertEquals(0, new BigDecimal("700.00").compareTo(afterWithdraw.getBalance()));

        // Check transaction history
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));

        // Verify transactions in database
        List<Transaction> transactions = transactionRepository.findByAccountId(afterWithdraw.getId());
        assertEquals(2, transactions.size());
    }

    // --- Transfer integration test ---

    @Test
    @WithMockUser(username = "sender")
    void transferFlow_shouldMoveMoneyBetweenAccounts() throws Exception {
        // Create sender and recipient accounts
        Account sender = accountService.registerAccount("sender", "password");
        Account recipient = accountService.registerAccount("recipient", "password");

        // Deposit funds to sender
        accountService.deposit(sender, new BigDecimal("1000.00"));

        // Transfer
        mockMvc.perform(post("/transfer")
                        .with(csrf())
                        .param("toUsername", "recipient")
                        .param("amount", "400.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        // Verify balances
        Account senderAfter = accountRepository.findByUsername("sender").get();
        Account recipientAfter = accountRepository.findByUsername("recipient").get();
        assertEquals(0, new BigDecimal("600.00").compareTo(senderAfter.getBalance()));
        assertEquals(0, new BigDecimal("400.00").compareTo(recipientAfter.getBalance()));

        // Verify transaction records created for both accounts
        List<Transaction> senderTxns = transactionRepository.findByAccountId(senderAfter.getId());
        List<Transaction> recipientTxns = transactionRepository.findByAccountId(recipientAfter.getId());
        assertTrue(senderTxns.size() >= 2); // deposit + transfer out
        assertTrue(recipientTxns.size() >= 1); // transfer in
    }

    // --- Insufficient funds integration test ---

    @Test
    @WithMockUser(username = "pooruser")
    void withdraw_shouldFailWithInsufficientFunds() throws Exception {
        accountService.registerAccount("pooruser", "password");

        mockMvc.perform(post("/withdraw")
                        .with(csrf())
                        .param("amount", "500.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "pooruser2")
    void transfer_shouldFailWithInsufficientFunds() throws Exception {
        accountService.registerAccount("pooruser2", "password");
        accountService.registerAccount("recipient2", "password");

        mockMvc.perform(post("/transfer")
                        .with(csrf())
                        .param("toUsername", "recipient2")
                        .param("amount", "500.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    // --- Dashboard integration test ---

    @Test
    @WithMockUser(username = "dashuser")
    void dashboard_shouldShowAccountInfo() throws Exception {
        accountService.registerAccount("dashuser", "password");

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("account"));
    }

    // --- Login/Registration page access ---

    @Test
    void loginPage_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void registerPage_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    // --- Service layer direct tests with real DB ---

    @Test
    void accountService_deposit_shouldPersistTransaction() {
        Account account = accountService.registerAccount("svcuser", "password");
        accountService.deposit(account, new BigDecimal("250.00"));

        Account updated = accountRepository.findByUsername("svcuser").get();
        assertEquals(0, new BigDecimal("250.00").compareTo(updated.getBalance()));

        List<Transaction> txns = transactionRepository.findByAccountId(updated.getId());
        assertEquals(1, txns.size());
        assertEquals("Deposit", txns.get(0).getType());
    }

    @Test
    void accountService_withdraw_shouldPersistTransaction() {
        Account account = accountService.registerAccount("svcuser2", "password");
        accountService.deposit(account, new BigDecimal("500.00"));
        accountService.withdraw(account, new BigDecimal("200.00"));

        Account updated = accountRepository.findByUsername("svcuser2").get();
        assertEquals(0, new BigDecimal("300.00").compareTo(updated.getBalance()));

        List<Transaction> txns = transactionRepository.findByAccountId(updated.getId());
        assertEquals(2, txns.size());
    }

    @Test
    void accountService_loadUserByUsername_shouldReturnUserDetails() {
        accountService.registerAccount("loaduser", "password");

        var userDetails = accountService.loadUserByUsername("loaduser");
        assertNotNull(userDetails);
        assertEquals("loaduser", userDetails.getUsername());
        assertFalse(userDetails.getAuthorities().isEmpty());
    }
}
