package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        testAccount = new Account();
        testAccount.setUsername("testuser");
        testAccount.setPassword(passwordEncoder.encode("password"));
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount = accountRepository.save(testAccount);
    }

    @Test
    @WithMockUser(username = "testuser")
    void deposit_shouldRedirectToDashboard() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "500"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        Account updated = accountRepository.findByUsername("testuser").orElseThrow();
        assert updated.getBalance().compareTo(new BigDecimal("1500.00")) == 0;
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_shouldRedirectToDashboard() throws Exception {
        mockMvc.perform(post("/withdraw").param("amount", "300"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        Account updated = accountRepository.findByUsername("testuser").orElseThrow();
        assert updated.getBalance().compareTo(new BigDecimal("700.00")) == 0;
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_withInsufficientFunds_shouldShowError() throws Exception {
        mockMvc.perform(post("/withdraw").param("amount", "2000"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transfer_shouldRedirectToDashboard() throws Exception {
        Account recipient = new Account();
        recipient.setUsername("recipient");
        recipient.setPassword(passwordEncoder.encode("password"));
        recipient.setBalance(new BigDecimal("500.00"));
        accountRepository.save(recipient);

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "200"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        Account updatedSender = accountRepository.findByUsername("testuser").orElseThrow();
        Account updatedRecipient = accountRepository.findByUsername("recipient").orElseThrow();
        assert updatedSender.getBalance().compareTo(new BigDecimal("800.00")) == 0;
        assert updatedRecipient.getBalance().compareTo(new BigDecimal("700.00")) == 0;
    }

    @Test
    @WithMockUser(username = "testuser")
    void transfer_toNonExistentUser_shouldShowError() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("toUsername", "nonexistent")
                        .param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transactions_shouldShowTransactionHistory() throws Exception {
        Transaction transaction = new Transaction(
                new BigDecimal("100.00"), "Deposit", LocalDateTime.now(), testAccount);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_shouldDisplayAccountInfo() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    void register_shouldCreateAccountAndRedirect() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "newpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assert accountRepository.findByUsername("newuser").isPresent();
    }

    @Test
    void register_withDuplicate_shouldShowError() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "testuser")
                        .param("password", "password"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }
}
