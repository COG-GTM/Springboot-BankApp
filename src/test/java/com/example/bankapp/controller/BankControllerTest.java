package com.example.bankapp.controller;

import com.example.bankapp.config.SecurityConfig;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankController.class)
@Import(SecurityConfig.class)
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    // ---- GET /register ----

    @Test
    void showRegistrationForm_shouldReturnRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    // ---- POST /register ----

    @Test
    void registerAccount_shouldRedirectToLoginOnSuccess() throws Exception {
        when(accountService.registerAccount("testuser", "password")).thenReturn(new Account());

        mockMvc.perform(post("/register")
                        .param("username", "testuser")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void registerAccount_shouldReturnRegisterViewOnError() throws Exception {
        when(accountService.registerAccount("existing", "password"))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                        .param("username", "existing")
                        .param("password", "password"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    // ---- GET /dashboard ----

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_shouldReturnDashboardView() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(BigDecimal.TEN);
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    void dashboard_shouldRedirectToLoginWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    // ---- POST /deposit ----

    @Test
    @WithMockUser(username = "testuser")
    void deposit_shouldRedirectToDashboard() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/deposit")
                        .param("amount", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    // ---- POST /withdraw ----

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_shouldRedirectToDashboard() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("100"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/withdraw")
                        .param("amount", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_shouldReturnDashboardWithErrorOnInsufficientFunds() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("10"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(any(Account.class), any(BigDecimal.class));

        mockMvc.perform(post("/withdraw")
                        .param("amount", "50"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    // ---- GET /transactions ----

    @Test
    @WithMockUser(username = "testuser")
    void transactions_shouldReturnTransactionsView() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        when(accountService.getTransactionHistory(account)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));
    }

    // ---- POST /transfer ----

    @Test
    @WithMockUser(username = "testuser")
    void transfer_shouldRedirectToDashboard() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("100"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "40"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transfer_shouldReturnDashboardWithErrorOnFailure() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("10"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).transferAmount(any(Account.class), eq("recipient"), any(BigDecimal.class));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "50"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }
}
