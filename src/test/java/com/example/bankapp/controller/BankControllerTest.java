package com.example.bankapp.controller;

import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void cleanUp() {
        accountRepository.deleteAll();
    }

    @Test
    void registerFormIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void registerSubmissionRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "pw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void registerDuplicateReturnsFormWithError() throws Exception {
        accountService.registerAccount("dupe", "pw");

        mockMvc.perform(post("/register")
                        .param("username", "dupe")
                        .param("password", "pw"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void protectedPageRedirectsToLoginWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "dashuser")
    void dashboardReturnsViewForAuthenticatedUser() throws Exception {
        accountService.registerAccount("dashuser", "pw");

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @WithMockUser(username = "depositor")
    void depositRedirectsToDashboardAndUpdatesBalance() throws Exception {
        accountService.registerAccount("depositor", "pw");

        mockMvc.perform(post("/deposit").param("amount", "150.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        assertBalance("depositor", "150.00");
    }

    @Test
    @WithMockUser(username = "withdrawer")
    void withdrawInsufficientFundsReturnsDashboardWithError() throws Exception {
        accountService.registerAccount("withdrawer", "pw");

        mockMvc.perform(post("/withdraw").param("amount", "999.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "viewer")
    void transactionsPageReturnsView() throws Exception {
        accountService.registerAccount("viewer", "pw");

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));
    }

    @Test
    @WithMockUser(username = "sender")
    void transferToUnknownRecipientReturnsDashboardWithError() throws Exception {
        accountService.registerAccount("sender", "pw");
        accountService.deposit(accountService.findAccountByUsername("sender"), new BigDecimal("100.00"));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "ghost")
                        .param("amount", "10.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    private void assertBalance(String username, String expected) {
        org.junit.jupiter.api.Assertions.assertEquals(0,
                new BigDecimal(expected).compareTo(accountService.findAccountByUsername(username).getBalance()));
    }
}
