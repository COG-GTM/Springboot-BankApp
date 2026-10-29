package com.example.bankapp.controller;

import com.example.bankapp.config.SecurityConfig;
import com.example.bankapp.model.Account;
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
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
        alice = new Account("alice", "encoded", new BigDecimal("500.00"), new ArrayList<>(), List.of());
        alice.setId(1L);
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);
    }

    @Test
    @WithMockUser(username = "alice")
    void depositRedirectsToDashboardAndDepositsIntoLoggedInUsersAccount() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "100").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).findAccountByUsername("alice");
        verify(accountService).deposit(alice, new BigDecimal("100"));
    }

    @Test
    @WithMockUser(username = "alice")
    void withdrawSuccessRedirectsToDashboard() throws Exception {
        mockMvc.perform(post("/withdraw").param("amount", "50").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).withdraw(alice, new BigDecimal("50"));
    }

    @Test
    @WithMockUser(username = "alice")
    void withdrawFailureRendersDashboardWithErrorAndAccount() throws Exception {
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(alice, new BigDecimal("1000"));

        mockMvc.perform(post("/withdraw").param("amount", "1000").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Insufficient funds"))
                .andExpect(model().attribute("account", alice));
    }

    @Test
    @WithMockUser(username = "alice")
    void transferSuccessRedirectsToDashboardAndTransfersToRecipient() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("toUsername", "bob")
                        .param("amount", "75")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(alice, "bob", new BigDecimal("75"));
    }

    @Test
    @WithMockUser(username = "alice")
    void transferFailureRendersDashboardWithErrorAndAccount() throws Exception {
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(alice, "carol", new BigDecimal("75"));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "carol")
                        .param("amount", "75")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Recipient account not found"))
                .andExpect(model().attribute("account", alice));
    }

    @Test
    void unauthenticatedDepositRedirectsToLoginAndIsNotProcessed() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "100").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verify(accountService, never()).findAccountByUsername(anyString());
        verify(accountService, never()).deposit(any(), any());
    }
}
