package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import com.example.bankapp.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    // --- GET /dashboard ---

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_authenticated() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("1000.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(view().name("dashboard"));
    }

    @Test
    void dashboard_unauthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    // --- GET /register ---

    @Test
    void showRegistrationForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    // --- POST /register ---

    @Test
    void registerAccount_success() throws Exception {
        when(accountService.registerAccount(anyString(), anyString())).thenReturn(new Account());

        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "pass123")
                        )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void registerAccount_duplicateUsername() throws Exception {
        when(accountService.registerAccount(anyString(), anyString()))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                        .param("username", "existing")
                        .param("password", "pass123")
                        )
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("register"));
    }

    // --- GET /login ---

    @Test
    void login() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    // --- POST /deposit ---

    @Test
    @WithMockUser(username = "testuser")
    void deposit_success() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("100.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/deposit")
                        .param("amount", "50.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).deposit(any(Account.class), eq(new BigDecimal("50.00")));
    }

    // --- POST /withdraw ---

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_success() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("100.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/withdraw")
                        .param("amount", "50.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_insufficientFunds() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("30.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(any(Account.class), any(BigDecimal.class));

        mockMvc.perform(post("/withdraw")
                        .param("amount", "50.00"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("account"))
                .andExpect(view().name("dashboard"));
    }

    // --- GET /transactions ---

    @Test
    @WithMockUser(username = "testuser")
    void transactionHistory() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setId(1L);
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        List<Transaction> transactions = List.of(
                new Transaction(new BigDecimal("100"), "Deposit", LocalDateTime.now(), account)
        );
        when(accountService.getTransactionHistory(account)).thenReturn(transactions);

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("transactions"))
                .andExpect(view().name("transactions"));
    }

    // --- POST /transfer ---

    @Test
    @WithMockUser(username = "testuser")
    void transfer_success() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("100.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "50.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(any(Account.class), eq("recipient"), eq(new BigDecimal("50.00")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transfer_insufficientFunds() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("30.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).transferAmount(any(Account.class), anyString(), any(BigDecimal.class));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "50.00"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("dashboard"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transfer_recipientNotFound() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("100.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(any(Account.class), anyString(), any(BigDecimal.class));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "unknown")
                        .param("amount", "50.00"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("dashboard"));
    }
}
