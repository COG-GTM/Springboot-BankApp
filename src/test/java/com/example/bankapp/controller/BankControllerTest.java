package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUsername("testuser");
        testAccount.setPassword("encodedPassword");
        testAccount.setBalance(new BigDecimal("1000.00"));
    }

    // --- Dashboard tests ---

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_shouldReturnDashboardView() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("account"));
    }

    // --- Registration tests ---

    @Test
    void showRegistrationForm_shouldReturnRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void registerAccount_shouldRedirectToLogin_onSuccess() throws Exception {
        when(accountService.registerAccount("newuser", "password123")).thenReturn(testAccount);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void registerAccount_shouldReturnRegisterView_onError() throws Exception {
        when(accountService.registerAccount("testuser", "password123"))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "testuser")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    // --- Login test ---

    @Test
    void login_shouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    // --- Deposit tests ---

    @Test
    @WithMockUser(username = "testuser")
    void deposit_shouldRedirectToDashboard() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);

        mockMvc.perform(post("/deposit")
                        .with(csrf())
                        .param("amount", "500.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).deposit(eq(testAccount), eq(new BigDecimal("500.00")));
    }

    // --- Withdraw tests ---

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_shouldRedirectToDashboard_onSuccess() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);

        mockMvc.perform(post("/withdraw")
                        .with(csrf())
                        .param("amount", "200.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).withdraw(eq(testAccount), eq(new BigDecimal("200.00")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_shouldReturnDashboard_onInsufficientFunds() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(any(Account.class), any(BigDecimal.class));

        mockMvc.perform(post("/withdraw")
                        .with(csrf())
                        .param("amount", "5000.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("account"));
    }

    // --- Transactions tests ---

    @Test
    @WithMockUser(username = "testuser")
    void transactionHistory_shouldReturnTransactionsView() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        Transaction t1 = new Transaction(new BigDecimal("100"), "Deposit", LocalDateTime.now(), testAccount);
        when(accountService.getTransactionHistory(testAccount)).thenReturn(Arrays.asList(t1));

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transactionHistory_shouldReturnEmptyList() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        when(accountService.getTransactionHistory(testAccount)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));
    }

    // --- Transfer tests ---

    @Test
    @WithMockUser(username = "testuser")
    void transfer_shouldRedirectToDashboard_onSuccess() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);

        mockMvc.perform(post("/transfer")
                        .with(csrf())
                        .param("toUsername", "recipient")
                        .param("amount", "200.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(eq(testAccount), eq("recipient"), eq(new BigDecimal("200.00")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transfer_shouldReturnDashboard_onError() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).transferAmount(any(Account.class), anyString(), any(BigDecimal.class));

        mockMvc.perform(post("/transfer")
                        .with(csrf())
                        .param("toUsername", "recipient")
                        .param("amount", "5000.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("account"));
    }

    // --- Unauthenticated access tests ---

    @Test
    void dashboard_shouldRedirectToLogin_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void deposit_shouldRedirectToLogin_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/deposit")
                        .with(csrf())
                        .param("amount", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void withdraw_shouldRedirectToLogin_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/withdraw")
                        .with(csrf())
                        .param("amount", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void transactions_shouldRedirectToLogin_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void transfer_shouldRedirectToLogin_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/transfer")
                        .with(csrf())
                        .param("toUsername", "someone")
                        .param("amount", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
