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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    private static final String USERNAME = "alice";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account(USERNAME, "encoded", new BigDecimal("100.00"), new ArrayList<>(), new ArrayList<>());
        account.setId(1L);
        when(accountService.findAccountByUsername(USERNAME)).thenReturn(account);
    }

    @Test
    @WithMockUser(username = USERNAME)
    void depositCallsServiceAndRedirectsToDashboard() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "50.00").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).deposit(account, new BigDecimal("50.00"));
    }

    @Test
    @WithMockUser(username = USERNAME)
    void depositWithoutCsrfTokenIsAcceptedBecauseCsrfIsDisabled() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).deposit(account, new BigDecimal("10"));
    }

    @Test
    @WithMockUser(username = USERNAME)
    void withdrawRedirectsToDashboardOnSuccess() throws Exception {
        mockMvc.perform(post("/withdraw").param("amount", "25.00").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).withdraw(account, new BigDecimal("25.00"));
    }

    @Test
    @WithMockUser(username = USERNAME)
    void withdrawRendersDashboardWithErrorAndAccountOnRuntimeException() throws Exception {
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(any(Account.class), any(BigDecimal.class));

        mockMvc.perform(post("/withdraw").param("amount", "500.00").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Insufficient funds"))
                .andExpect(model().attribute("account", account));
    }

    @Test
    @WithMockUser(username = USERNAME)
    void transferPassesToUsernameAndAmountAndRedirectsOnSuccess() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("toUsername", "bob")
                        .param("amount", "30.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(account, "bob", new BigDecimal("30.00"));
    }

    @Test
    @WithMockUser(username = USERNAME)
    void transferRendersDashboardWithErrorAndAccountOnRuntimeException() throws Exception {
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(any(Account.class), anyString(), any(BigDecimal.class));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "nobody")
                        .param("amount", "30.00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Recipient account not found"))
                .andExpect(model().attribute("account", account));

        verify(accountService).transferAmount(account, "nobody", new BigDecimal("30.00"));
    }

    @Test
    void unauthenticatedDepositRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "50.00").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(accountService);
    }

    @Test
    void unauthenticatedWithdrawRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/withdraw").param("amount", "50.00").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(accountService);
    }

    @Test
    void unauthenticatedTransferRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("toUsername", "bob")
                        .param("amount", "50.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verify(accountService, never()).transferAmount(any(), anyString(), any());
    }

    @Test
    @WithMockUser(username = USERNAME)
    void negativeDepositIsPassedThroughToServiceUnvalidated() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "-40.00").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).deposit(account, new BigDecimal("-40.00"));
    }
}
