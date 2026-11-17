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
import java.util.Collections;
import java.util.List;

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
        testAccount.setUsername("user");
        testAccount.setPassword("encoded");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setTransactions(Collections.emptyList());
    }

    // ── dashboard ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user")
    void dashboard_authenticatedUser_returnsViewWithAccount() throws Exception {
        when(accountService.findAccountByUsername("user")).thenReturn(testAccount);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attribute("account", testAccount));

        verify(accountService).findAccountByUsername("user");
    }

    @Test
    void dashboard_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    // ── showRegistrationForm ────────────────────────────────────────────

    @Test
    @WithMockUser
    void showRegistrationForm_returnsRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    // ── registerAccount ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    void registerAccount_success_redirectsToLogin() throws Exception {
        when(accountService.registerAccount("newuser", "pass123")).thenReturn(testAccount);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("password", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(accountService).registerAccount("newuser", "pass123");
    }

    @Test
    @WithMockUser
    void registerAccount_duplicateUser_returnsRegisterWithError() throws Exception {
        when(accountService.registerAccount("john", "pass"))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "john")
                        .param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Username already exists"));
    }

    // ── login ───────────────────────────────────────────────────────────

    @Test
    void login_returnsOk() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    // ── deposit ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user")
    void deposit_success_redirectsToDashboard() throws Exception {
        when(accountService.findAccountByUsername("user")).thenReturn(testAccount);

        mockMvc.perform(post("/deposit")
                        .with(csrf())
                        .param("amount", "500.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).findAccountByUsername("user");
        verify(accountService).deposit(eq(testAccount), eq(new BigDecimal("500.00")));
    }

    // ── withdraw ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user")
    void withdraw_success_redirectsToDashboard() throws Exception {
        when(accountService.findAccountByUsername("user")).thenReturn(testAccount);

        mockMvc.perform(post("/withdraw")
                        .with(csrf())
                        .param("amount", "200.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).withdraw(eq(testAccount), eq(new BigDecimal("200.00")));
    }

    @Test
    @WithMockUser(username = "user")
    void withdraw_insufficientFunds_returnsDashboardWithError() throws Exception {
        when(accountService.findAccountByUsername("user")).thenReturn(testAccount);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(eq(testAccount), eq(new BigDecimal("9999.00")));

        mockMvc.perform(post("/withdraw")
                        .with(csrf())
                        .param("amount", "9999.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Insufficient funds"))
                .andExpect(model().attribute("account", testAccount));
    }

    // ── transactionHistory ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user")
    void transactionHistory_returnsTransactionsView() throws Exception {
        when(accountService.findAccountByUsername("user")).thenReturn(testAccount);
        List<Transaction> txList = List.of(
                new Transaction(new BigDecimal("100"), "Deposit", LocalDateTime.now(), testAccount)
        );
        when(accountService.getTransactionHistory(testAccount)).thenReturn(txList);

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"))
                .andExpect(model().attribute("transactions", txList));
    }

    // ── transferAmount ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user")
    void transfer_success_redirectsToDashboard() throws Exception {
        when(accountService.findAccountByUsername("user")).thenReturn(testAccount);

        mockMvc.perform(post("/transfer")
                        .with(csrf())
                        .param("toUsername", "jane")
                        .param("amount", "300.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(eq(testAccount), eq("jane"), eq(new BigDecimal("300.00")));
    }

    @Test
    @WithMockUser(username = "user")
    void transfer_insufficientFunds_returnsDashboardWithError() throws Exception {
        when(accountService.findAccountByUsername("user")).thenReturn(testAccount);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).transferAmount(eq(testAccount), eq("jane"), eq(new BigDecimal("9999.00")));

        mockMvc.perform(post("/transfer")
                        .with(csrf())
                        .param("toUsername", "jane")
                        .param("amount", "9999.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Insufficient funds"))
                .andExpect(model().attribute("account", testAccount));
    }

    @Test
    @WithMockUser(username = "user")
    void transfer_recipientNotFound_returnsDashboardWithError() throws Exception {
        when(accountService.findAccountByUsername("user")).thenReturn(testAccount);
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(eq(testAccount), eq("ghost"), eq(new BigDecimal("100.00")));

        mockMvc.perform(post("/transfer")
                        .with(csrf())
                        .param("toUsername", "ghost")
                        .param("amount", "100.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Recipient account not found"))
                .andExpect(model().attribute("account", testAccount));
    }
}
