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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(BankController.class)
@Import(SecurityConfig.class)
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    private Account alice;

    @BeforeEach
    void setUp() {
        alice = new Account();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setBalance(new BigDecimal("100.00"));
    }

    @Test
    void deposit_callsServiceWithAuthenticatedAccountAndRedirects() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);

        mockMvc.perform(post("/deposit").param("amount", "25.00").with(user("alice")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).findAccountByUsername("alice");
        verify(accountService).deposit(alice, new BigDecimal("25.00"));
    }

    @Test
    void withdraw_successRedirectsToDashboard() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);

        mockMvc.perform(post("/withdraw").param("amount", "40.00").with(user("alice")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).withdraw(alice, new BigDecimal("40.00"));
    }

    @Test
    void withdraw_failureRendersDashboardWithErrorAndAccount() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(eq(alice), any(BigDecimal.class));

        mockMvc.perform(post("/withdraw").param("amount", "500.00").with(user("alice")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Insufficient funds"))
                .andExpect(model().attribute("account", alice));
    }

    @Test
    void transfer_successRedirectsToDashboard() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "bob")
                        .param("amount", "30.00")
                        .with(user("alice"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(alice, "bob", new BigDecimal("30.00"));
    }

    @Test
    void transfer_failureRendersDashboardWithErrorAndAccount() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(eq(alice), eq("ghost"), any(BigDecimal.class));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "ghost")
                        .param("amount", "10.00")
                        .with(user("alice"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Recipient account not found"))
                .andExpect(model().attribute("account", alice));
    }

    @Test
    @WithMockUser(username = "alice")
    void transactions_rendersHistoryForAuthenticatedAccount() throws Exception {
        Transaction deposit = new Transaction(new BigDecimal("25.00"), "Deposit", null, alice);
        List<Transaction> history = List.of(deposit);
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);
        when(accountService.getTransactionHistory(alice)).thenReturn(history);

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attribute("transactions", history));
    }

    @Test
    void unauthenticatedDeposit_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "25.00").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(accountService);
    }

    @Test
    void unauthenticatedWithdraw_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/withdraw").param("amount", "25.00").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(accountService);
    }

    @Test
    void unauthenticatedTransfer_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "25.00").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(accountService);
    }
}
