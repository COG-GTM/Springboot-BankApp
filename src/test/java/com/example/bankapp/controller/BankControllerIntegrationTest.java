package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
    private AccountService accountService;

    @Test
    void dashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void deposit_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void withdraw_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/withdraw").param("amount", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void transfer_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "other").param("amount", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void register_validCredentials_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "password123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void register_duplicateUsername_showsError() throws Exception {
        accountService.registerAccount("existing", "password123");

        mockMvc.perform(post("/register")
                        .param("username", "existing")
                        .param("password", "password123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "deposituser")
    void deposit_authenticated_updatesBalance() throws Exception {
        accountService.registerAccount("deposituser", "password123");

        mockMvc.perform(post("/deposit")
                        .param("amount", "500")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        Account updated = accountService.findAccountByUsername("deposituser");
        assert updated.getBalance().compareTo(new BigDecimal("500")) == 0;
    }

    @Test
    @WithMockUser(username = "withdrawuser")
    void withdraw_authenticated_insufficientFunds_showsError() throws Exception {
        accountService.registerAccount("withdrawuser", "password123");

        mockMvc.perform(post("/withdraw")
                        .param("amount", "1000")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "sender")
    void transfer_authenticated_happyPath() throws Exception {
        accountService.registerAccount("sender", "password123");
        accountService.registerAccount("receiver", "password123");

        Account sender = accountService.findAccountByUsername("sender");
        accountService.deposit(sender, new BigDecimal("1000"));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "receiver")
                        .param("amount", "300")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        Account updatedSender = accountService.findAccountByUsername("sender");
        Account updatedReceiver = accountService.findAccountByUsername("receiver");
        assert updatedSender.getBalance().compareTo(new BigDecimal("700")) == 0;
        assert updatedReceiver.getBalance().compareTo(new BigDecimal("300")) == 0;
    }

    @Test
    @WithMockUser(username = "transferuser")
    void transfer_authenticated_recipientNotFound_showsError() throws Exception {
        accountService.registerAccount("transferuser", "password123");

        Account user = accountService.findAccountByUsername("transferuser");
        accountService.deposit(user, new BigDecimal("1000"));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "nonexistent")
                        .param("amount", "100")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }
}
