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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
    @WithMockUser(username = "alice")
    void dashboardShowsSignedInAccount() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("account", alice));
    }

    @Test
    @WithMockUser(username = "alice")
    void depositRedirectsToDashboard() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);

        mockMvc.perform(post("/deposit").param("amount", "25.00"))
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).deposit(alice, new BigDecimal("25.00"));
    }

    @Test
    @WithMockUser(username = "alice")
    void withdrawRedirectsToDashboardOnSuccess() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);

        mockMvc.perform(post("/withdraw").param("amount", "10.00"))
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).withdraw(alice, new BigDecimal("10.00"));
    }

    @Test
    @WithMockUser(username = "alice")
    void withdrawShowsErrorOnDashboardWhenFundsAreInsufficient() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(any(Account.class), any(BigDecimal.class));

        mockMvc.perform(post("/withdraw").param("amount", "500.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Insufficient funds"))
                .andExpect(model().attribute("account", alice));
    }

    @Test
    @WithMockUser(username = "alice")
    void transferRedirectsToDashboardOnSuccess() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);

        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "40.00"))
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(alice, "bob", new BigDecimal("40.00"));
    }

    @Test
    @WithMockUser(username = "alice")
    void transferShowsErrorOnDashboardWhenRecipientIsUnknown() throws Exception {
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(any(Account.class), any(String.class), any(BigDecimal.class));

        mockMvc.perform(post("/transfer").param("toUsername", "ghost").param("amount", "40.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Recipient account not found"));
    }

    @Test
    @WithMockUser(username = "alice")
    void transactionsListsHistory() throws Exception {
        Transaction deposit = new Transaction(new BigDecimal("25.00"), "Deposit", LocalDateTime.now(), alice);
        deposit.setId(7L);
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);
        when(accountService.getTransactionHistory(alice)).thenReturn(List.of(deposit));

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attribute("transactions", List.of(deposit)));
    }

    @Test
    void registerRedirectsToLoginOnSuccess() throws Exception {
        mockMvc.perform(post("/register").param("username", "carol").param("password", "secret"))
                .andExpect(redirectedUrl("/login"));

        verify(accountService).registerAccount("carol", "secret");
    }

    @Test
    void registerShowsErrorWhenUsernameIsTaken() throws Exception {
        when(accountService.registerAccount("alice", "secret"))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register").param("username", "alice").param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Username already exists"));
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void unauthenticatedMoneyMovementRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "25.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(post("/withdraw").param("amount", "25.00"))
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "25.00"))
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(accountService);
    }
}
