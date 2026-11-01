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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    // --- GET /login ---

    @Test
    @WithMockUser
    void login_ReturnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    // --- GET /register ---

    @Test
    void showRegistrationForm_ReturnsRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    // --- POST /register ---

    @Test
    void registerAccount_Success() throws Exception {
        Account account = new Account();
        account.setUsername("newuser");
        when(accountService.registerAccount("newuser", "password")).thenReturn(account);

        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verify(accountService).registerAccount("newuser", "password");
    }

    @Test
    void registerAccount_UsernameExists() throws Exception {
        when(accountService.registerAccount("existinguser", "password"))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                        .param("username", "existinguser")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    // --- GET /dashboard ---

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_Authenticated() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("1000.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("account"));
        verify(accountService).findAccountByUsername("testuser");
    }

    @Test
    void dashboard_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // --- POST /deposit ---

    @Test
    @WithMockUser(username = "testuser")
    void deposit_Success() throws Exception {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("500.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/deposit")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
        verify(accountService).deposit(eq(account), eq(new BigDecimal("100.00")));
    }

    @Test
    void deposit_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/deposit")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // --- POST /withdraw ---

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_Success() throws Exception {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("500.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/withdraw")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
        verify(accountService).withdraw(eq(account), eq(new BigDecimal("100.00")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_InsufficientFunds() throws Exception {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("50.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(eq(account), eq(new BigDecimal("100.00")));

        mockMvc.perform(post("/withdraw")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    void withdraw_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/withdraw")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // --- GET /transactions ---

    @Test
    @WithMockUser(username = "testuser")
    void transactionHistory_Success() throws Exception {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        when(accountService.getTransactionHistory(account)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transactionHistory_WithTransactions() throws Exception {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        Transaction t1 = new Transaction(new BigDecimal("100"), "Deposit", LocalDateTime.now(), account);
        Transaction t2 = new Transaction(new BigDecimal("50"), "Withdrawal", LocalDateTime.now(), account);
        when(accountService.getTransactionHistory(account)).thenReturn(Arrays.asList(t1, t2));

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));
    }

    @Test
    void transactionHistory_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // --- POST /transfer ---

    @Test
    @WithMockUser(username = "testuser")
    void transferAmount_Success() throws Exception {
        Account fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setUsername("testuser");
        fromAccount.setBalance(new BigDecimal("500.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(fromAccount);

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "receiver")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
        verify(accountService).transferAmount(eq(fromAccount), eq("receiver"), eq(new BigDecimal("100.00")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transferAmount_InsufficientFunds() throws Exception {
        Account fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setUsername("testuser");
        fromAccount.setBalance(new BigDecimal("50.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(fromAccount);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).transferAmount(eq(fromAccount), eq("receiver"), eq(new BigDecimal("100.00")));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "receiver")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transferAmount_RecipientNotFound() throws Exception {
        Account fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setUsername("testuser");
        fromAccount.setBalance(new BigDecimal("500.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(fromAccount);
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(eq(fromAccount), eq("unknown"), eq(new BigDecimal("100.00")));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "unknown")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    void transferAmount_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("toUsername", "receiver")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
