package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankController.class)
@AutoConfigureMockMvc(addFilters = false)
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    // --- GET endpoints ---

    @Test
    @WithMockUser(username = "testuser")
    void testDashboard_Authenticated() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(BigDecimal.valueOf(500));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    void testShowRegistrationForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void testLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testTransactionHistory_Authenticated() throws Exception {
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

    // --- POST /register ---

    @Test
    void testRegisterAccount_Success() throws Exception {
        Account account = new Account();
        account.setUsername("newuser");
        when(accountService.registerAccount("newuser", "pass")).thenReturn(account);

        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testRegisterAccount_Failure() throws Exception {
        when(accountService.registerAccount("existinguser", "pass"))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                        .param("username", "existinguser")
                        .param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    // --- POST /deposit ---

    @Test
    @WithMockUser(username = "testuser")
    void testDeposit_Success() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(BigDecimal.valueOf(100));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/deposit")
                        .param("amount", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).deposit(account, BigDecimal.valueOf(100));
    }

    // --- POST /withdraw ---

    @Test
    @WithMockUser(username = "testuser")
    void testWithdraw_Success() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(BigDecimal.valueOf(200));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/withdraw")
                        .param("amount", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).withdraw(account, BigDecimal.valueOf(50));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testWithdraw_InsufficientFunds() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(BigDecimal.valueOf(50));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(account, BigDecimal.valueOf(100));

        mockMvc.perform(post("/withdraw")
                        .param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    // --- POST /transfer ---

    @Test
    @WithMockUser(username = "testuser")
    void testTransfer_Success() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(BigDecimal.valueOf(200));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(account, "recipient", BigDecimal.valueOf(50));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testTransfer_Failure() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(BigDecimal.valueOf(10));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).transferAmount(account, "recipient", BigDecimal.valueOf(100));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }
}
