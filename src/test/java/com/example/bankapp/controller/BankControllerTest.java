package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankController.class)
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
        testAccount.setPassword("encodedpassword");
        testAccount.setBalance(new BigDecimal("1000.00"));
    }

    // --- Dashboard tests ---

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_success() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("account"));
    }

    // --- Registration tests ---

    @Test
    @WithMockUser
    void showRegistrationForm_success() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    @WithMockUser
    void registerAccount_success() throws Exception {
        when(accountService.registerAccount("newuser", "password")).thenReturn(testAccount);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser
    void registerAccount_usernameExists() throws Exception {
        when(accountService.registerAccount("testuser", "password"))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "testuser")
                        .param("password", "password"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    // --- Login tests ---

    @Test
    @WithMockUser
    void login_returnsOk() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    // --- Deposit tests ---

    @Test
    @WithMockUser(username = "testuser")
    void deposit_success() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doNothing().when(accountService).deposit(any(Account.class), any(BigDecimal.class));

        mockMvc.perform(post("/deposit")
                        .with(csrf())
                        .param("amount", "500.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).deposit(any(Account.class), eq(new BigDecimal("500.00")));
    }

    // --- Withdraw tests ---

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_success() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doNothing().when(accountService).withdraw(any(Account.class), any(BigDecimal.class));

        mockMvc.perform(post("/withdraw")
                        .with(csrf())
                        .param("amount", "200.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_insufficientFunds() throws Exception {
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

    // --- Transaction history tests ---

    @Test
    @WithMockUser(username = "testuser")
    void transactionHistory_success() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        List<Transaction> transactions = Arrays.asList(
                new Transaction(new BigDecimal("100"), "Deposit", LocalDateTime.now(), testAccount)
        );
        when(accountService.getTransactionHistory(any(Account.class))).thenReturn(transactions);

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));
    }

    // --- Transfer tests ---

    @Test
    @WithMockUser(username = "testuser")
    void transfer_success() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doNothing().when(accountService).transferAmount(any(Account.class), anyString(), any(BigDecimal.class));

        mockMvc.perform(post("/transfer")
                        .with(csrf())
                        .param("toUsername", "recipient")
                        .param("amount", "300.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transfer_insufficientFunds() throws Exception {
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
    void transfer_recipientNotFound() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(any(Account.class), anyString(), any(BigDecimal.class));

        mockMvc.perform(post("/transfer")
                        .with(csrf())
                        .param("toUsername", "unknown")
                        .param("amount", "100.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    // --- Unauthenticated access tests ---

    @Test
    void dashboard_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deposit_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/deposit")
                        .with(csrf())
                        .param("amount", "100.00"))
                .andExpect(status().isUnauthorized());
    }
}
