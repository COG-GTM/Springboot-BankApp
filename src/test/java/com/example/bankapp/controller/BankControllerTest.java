package com.example.bankapp.controller;

import com.example.bankapp.config.SecurityConfig;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUsername("testuser");
        testAccount.setPassword("password");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setTransactions(new ArrayList<>());
    }

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_Success() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("account"));

        verify(accountService).findAccountByUsername("testuser");
    }

    @Test
    void showRegistrationForm_Success() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void registerAccount_Success() throws Exception {
        when(accountService.registerAccount("newuser", "password123")).thenReturn(testAccount);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(accountService).registerAccount("newuser", "password123");
    }

    @Test
    void registerAccount_UsernameExists() throws Exception {
        when(accountService.registerAccount("existinguser", "password123"))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "existinguser")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void login_Success() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void deposit_Success() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doNothing().when(accountService).deposit(any(Account.class), any(BigDecimal.class));

        mockMvc.perform(post("/deposit")
                        .with(csrf())
                        .param("amount", "500.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).findAccountByUsername("testuser");
        verify(accountService).deposit(any(Account.class), any(BigDecimal.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_Success() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doNothing().when(accountService).withdraw(any(Account.class), any(BigDecimal.class));

        mockMvc.perform(post("/withdraw")
                        .with(csrf())
                        .param("amount", "200.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).findAccountByUsername("testuser");
        verify(accountService).withdraw(any(Account.class), any(BigDecimal.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_InsufficientFunds() throws Exception {
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

    @Test
    @WithMockUser(username = "testuser")
    void transactionHistory_Success() throws Exception {
        List<Transaction> transactions = new ArrayList<>();
        Transaction t1 = new Transaction();
        t1.setId(1L);
        t1.setAmount(new BigDecimal("100.00"));
        t1.setType("Deposit");
        t1.setTimestamp(LocalDateTime.now());
        transactions.add(t1);

        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        when(accountService.getTransactionHistory(testAccount)).thenReturn(transactions);

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));

        verify(accountService).findAccountByUsername("testuser");
        verify(accountService).getTransactionHistory(testAccount);
    }

    @Test
    @WithMockUser(username = "testuser")
    void transferAmount_Success() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doNothing().when(accountService).transferAmount(any(Account.class), anyString(), any(BigDecimal.class));

        mockMvc.perform(post("/transfer")
                        .with(csrf())
                        .param("toUsername", "recipient")
                        .param("amount", "100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).findAccountByUsername("testuser");
        verify(accountService).transferAmount(any(Account.class), eq("recipient"), any(BigDecimal.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transferAmount_InsufficientFunds() throws Exception {
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

    @Test
    @WithMockUser(username = "testuser")
    void transferAmount_RecipientNotFound() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(any(Account.class), anyString(), any(BigDecimal.class));

        mockMvc.perform(post("/transfer")
                        .with(csrf())
                        .param("toUsername", "nonexistent")
                        .param("amount", "100.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }
}
