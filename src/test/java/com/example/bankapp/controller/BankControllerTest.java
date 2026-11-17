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
        testAccount.setTransactions(Collections.emptyList());
    }

    @Test
    void register_GET_showsForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void register_POST_success_redirectsToLogin() throws Exception {
        when(accountService.registerAccount("newuser", "password123")).thenReturn(testAccount);

        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "password123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void register_POST_duplicateUsername_showsError() throws Exception {
        when(accountService.registerAccount("testuser", "password123"))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                        .param("username", "testuser")
                        .param("password", "password123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void dashboard_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "testuser")
    void deposit_POST_updatesBalance() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doNothing().when(accountService).deposit(any(Account.class), any(BigDecimal.class));

        mockMvc.perform(post("/deposit")
                        .param("amount", "500.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).deposit(any(Account.class), eq(new BigDecimal("500.00")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_POST_updatesBalance() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doNothing().when(accountService).withdraw(any(Account.class), any(BigDecimal.class));

        mockMvc.perform(post("/withdraw")
                        .param("amount", "200.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).withdraw(any(Account.class), eq(new BigDecimal("200.00")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void withdraw_POST_insufficientFunds_showsError() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(any(Account.class), any(BigDecimal.class));

        mockMvc.perform(post("/withdraw")
                        .param("amount", "5000.00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void transactions_GET_showsHistory() throws Exception {
        Transaction t1 = new Transaction(new BigDecimal("100.00"), "Deposit",
                LocalDateTime.now(), testAccount);
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        when(accountService.getTransactionHistory(any(Account.class)))
                .thenReturn(Arrays.asList(t1));

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));
    }
}
