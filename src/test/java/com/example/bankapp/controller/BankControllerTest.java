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
import java.util.Collections;

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
    void dashboard_success() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(BigDecimal.TEN);
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("account"));
    }

    // --- GET /register ---

    @Test
    void showRegistrationForm_success() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    // --- POST /register ---

    @Test
    void registerAccount_success() throws Exception {
        when(accountService.registerAccount("newuser", "pass")).thenReturn(new Account());

        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void registerAccount_usernameExists() throws Exception {
        when(accountService.registerAccount("existing", "pass"))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                        .param("username", "existing")
                        .param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    // --- GET /login ---

    @Test
    void login_success() throws Exception {
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
        account.setBalance(new BigDecimal("100"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/deposit")
                        .param("amount", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).deposit(eq(account), eq(new BigDecimal("50")));
    }

    // --- POST /withdraw ---

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_success() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("200"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/withdraw")
                        .param("amount", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).withdraw(eq(account), eq(new BigDecimal("50")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_insufficientFunds() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("10"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(eq(account), eq(new BigDecimal("100")));

        mockMvc.perform(post("/withdraw")
                        .param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("account"));
    }

    // --- GET /transactions ---

    @Test
    @WithMockUser(username = "testuser")
    void transactionHistory_success() throws Exception {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        Transaction tx = new Transaction(new BigDecimal("50"), "Deposit", LocalDateTime.now(), account);
        when(accountService.getTransactionHistory(account)).thenReturn(Collections.singletonList(tx));

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));
    }

    // --- POST /transfer ---

    @Test
    @WithMockUser(username = "testuser")
    void transferAmount_success() throws Exception {
        Account fromAccount = new Account();
        fromAccount.setUsername("testuser");
        fromAccount.setBalance(new BigDecimal("500"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(fromAccount);

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "receiver")
                        .param("amount", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(eq(fromAccount), eq("receiver"), eq(new BigDecimal("100")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transferAmount_insufficientFunds() throws Exception {
        Account fromAccount = new Account();
        fromAccount.setUsername("testuser");
        fromAccount.setBalance(new BigDecimal("10"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(fromAccount);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).transferAmount(eq(fromAccount), eq("receiver"), eq(new BigDecimal("1000")));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "receiver")
                        .param("amount", "1000"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transferAmount_recipientNotFound() throws Exception {
        Account fromAccount = new Account();
        fromAccount.setUsername("testuser");
        fromAccount.setBalance(new BigDecimal("500"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(fromAccount);
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(eq(fromAccount), eq("unknown"), eq(new BigDecimal("100")));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "unknown")
                        .param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("account"));
    }
}
