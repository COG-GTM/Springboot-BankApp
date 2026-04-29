package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BankControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();

        Account account = new Account();
        account.setUsername("testuser");
        account.setPassword(passwordEncoder.encode("password"));
        account.setBalance(new BigDecimal("1000"));
        accountRepository.save(account);

        Account recipient = new Account();
        recipient.setUsername("recipient");
        recipient.setPassword(passwordEncoder.encode("password"));
        recipient.setBalance(new BigDecimal("500"));
        accountRepository.save(recipient);
    }

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_authenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    void dashboard_unauthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "testuser")
    void deposit_success() throws Exception {
        mockMvc.perform(post("/deposit")
                        .param("amount", "500")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_success() throws Exception {
        mockMvc.perform(post("/withdraw")
                        .param("amount", "500")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_insufficientFunds() throws Exception {
        mockMvc.perform(post("/withdraw")
                        .param("amount", "5000")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transfer_success() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "300")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transfer_recipientNotFound() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("toUsername", "nonexistent")
                        .param("amount", "100")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void register_success() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "newpassword")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void register_duplicateUsername() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "testuser")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transactionHistory() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));
    }
}
