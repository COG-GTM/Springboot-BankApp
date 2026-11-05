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
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(BankController.class)
@Import(SecurityConfig.class)
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setUsername("alice");
        account.setBalance(new BigDecimal("100.00"));
    }

    @Test
    @WithMockUser(username = "alice")
    void dashboardRendersAccount() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(account);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("account", account));
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void registrationFormIsPublic() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void registerRedirectsToLoginOnSuccess() throws Exception {
        mockMvc.perform(post("/register").param("username", "bob").param("password", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(accountService).registerAccount("bob", "secret");
    }

    @Test
    void registerShowsErrorWhenUsernameTaken() throws Exception {
        when(accountService.registerAccount(eq("alice"), any()))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register").param("username", "alice").param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Username already exists"));
    }

    @Test
    @WithMockUser(username = "alice")
    void depositRedirectsToDashboard() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(account);

        mockMvc.perform(post("/deposit").param("amount", "50.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).deposit(account, new BigDecimal("50.00"));
    }

    @Test
    void depositRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "50.00"))
                .andExpect(status().is3xxRedirection());

        verify(accountService, never()).deposit(any(), any());
    }

    @Test
    @WithMockUser(username = "alice")
    void withdrawRedirectsToDashboard() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(account);

        mockMvc.perform(post("/withdraw").param("amount", "10.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).withdraw(account, new BigDecimal("10.00"));
    }

    @Test
    @WithMockUser(username = "alice")
    void withdrawRendersDashboardWithErrorOnInsufficientFunds() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds")).when(accountService).withdraw(any(), any());

        mockMvc.perform(post("/withdraw").param("amount", "999.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Insufficient funds"))
                .andExpect(model().attribute("account", account));
    }

    @Test
    @WithMockUser(username = "alice")
    void transactionHistoryIsRendered() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(account);
        when(accountService.getTransactionHistory(account)).thenReturn(Collections.singletonList(
                new Transaction(BigDecimal.TEN, "Deposit", LocalDateTime.now(), account)));

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));
    }

    @Test
    @WithMockUser(username = "alice")
    void transferRedirectsToDashboard() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(account);

        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "20.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(account, "bob", new BigDecimal("20.00"));
    }

    @Test
    @WithMockUser(username = "alice")
    void transferRendersDashboardWithErrorWhenRecipientMissing() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(account);
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(any(), any(), any());

        mockMvc.perform(post("/transfer").param("toUsername", "nobody").param("amount", "20.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Recipient account not found"));
    }

    @Test
    void loginPageIsReachable() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }
}
