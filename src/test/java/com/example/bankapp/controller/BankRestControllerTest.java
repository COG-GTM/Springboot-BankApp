package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BankRestControllerTest {

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

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testuser")
    void deposit_shouldReturnJsonWithUpdatedBalance() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/deposit").param("amount", "500"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deposit successful"))
                .andExpect(jsonPath("$.newBalance").value(1500.00))
                .andExpect(jsonPath("$.amount").value(500));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_shouldReturnJsonWithUpdatedBalance() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/withdraw").param("amount", "300"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Withdrawal successful"))
                .andExpect(jsonPath("$.newBalance").value(700.00))
                .andExpect(jsonPath("$.amount").value(300));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_withInsufficientFunds_shouldReturnError() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/withdraw").param("amount", "2000"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient funds"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transfer_shouldReturnJsonWithConfirmation() throws Exception {
        Account recipient = new Account();
        recipient.setUsername("recipient");
        recipient.setPassword(passwordEncoder.encode("password"));
        recipient.setBalance(new BigDecimal("500.00"));
        accountRepository.save(recipient);

        MvcResult result = mockMvc.perform(post("/api/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "200"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transfer successful"))
                .andExpect(jsonPath("$.newBalance").value(800.00))
                .andExpect(jsonPath("$.recipient").value("recipient"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transfer_toNonExistentUser_shouldReturnError() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transfer")
                        .param("toUsername", "nonexistent")
                        .param("amount", "100"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Recipient account not found"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transactions_shouldReturnJsonList() throws Exception {
        Transaction transaction = new Transaction(
                new BigDecimal("100.00"), "Deposit", LocalDateTime.now(), testAccount);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        MvcResult result = mockMvc.perform(get("/api/transactions"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("Deposit"))
                .andExpect(jsonPath("$[0].amount").value(100.00));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transfer_withInsufficientFunds_shouldReturnError() throws Exception {
        Account recipient = new Account();
        recipient.setUsername("recipient");
        recipient.setPassword(passwordEncoder.encode("password"));
        recipient.setBalance(new BigDecimal("500.00"));
        accountRepository.save(recipient);

        MvcResult result = mockMvc.perform(post("/api/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "5000"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient funds"));
    }
}
