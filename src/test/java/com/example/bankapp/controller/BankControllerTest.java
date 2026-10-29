package com.example.bankapp.controller;

import com.example.bankapp.config.SecurityConfig;
import com.example.bankapp.model.Account;
import com.example.bankapp.service.AccountService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);
    }

    @Test
    @WithMockUser(username = "alice")
    void transferHappyPathRedirectsToDashboard() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "25.00"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).findAccountByUsername("alice");
        verify(accountService).transferAmount(same(alice), eq("bob"),
                argThat(a -> a != null && a.compareTo(new BigDecimal("25.00")) == 0));
    }

    @Test
    @WithMockUser(username = "alice")
    void transferInsufficientFundsRendersDashboardWithError() throws Exception {
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).transferAmount(any(), anyString(), any());

        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "500.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Insufficient funds"))
                .andExpect(model().attribute("account", alice));
    }

    @Test
    @WithMockUser(username = "alice")
    void transferToUnknownRecipientRendersDashboardWithError() throws Exception {
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(any(), anyString(), any());

        mockMvc.perform(post("/transfer").param("toUsername", "ghost").param("amount", "10.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Recipient account not found"))
                .andExpect(model().attribute("account", alice));
    }

    @Test
    @WithMockUser(username = "alice")
    void transferWithoutToUsernameIsBadRequest() throws Exception {
        mockMvc.perform(post("/transfer").param("amount", "25.00"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @Test
    @WithMockUser(username = "alice")
    void transferWithoutAmountIsBadRequest() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @Test
    @WithMockUser(username = "alice")
    void transferWithNonNumericAmountIsBadRequest() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @Test
    void unauthenticatedTransferRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "25.00"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(accountService);
    }

    // LIKELY BUG: CSRF protection is disabled, so a money-moving POST succeeds without a token.
    @Test
    @WithMockUser(username = "alice")
    void transferWithoutCsrfTokenSucceeds() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "25.00"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(same(alice), eq("bob"), any());
    }

    // LIKELY BUG: negative amounts reach the service with no controller-level validation.
    @Test
    @WithMockUser(username = "alice")
    void negativeAmountIsPassedStraightToService() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "-10"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(same(alice), eq("bob"),
                argThat(a -> a != null && a.compareTo(new BigDecimal("-10")) == 0));
    }

    // LIKELY BUG: zero amounts reach the service with no controller-level validation.
    @Test
    @WithMockUser(username = "alice")
    void zeroAmountIsPassedStraightToService() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "0"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(same(alice), eq("bob"),
                argThat(a -> a != null && a.compareTo(BigDecimal.ZERO) == 0));
    }

    // LIKELY BUG: sender lookup is outside the try block, so its failure escapes as an unhandled 500.
    @Test
    @WithMockUser(username = "alice")
    void senderLookupFailurePropagatesUnhandled() {
        when(accountService.findAccountByUsername("alice"))
                .thenThrow(new RuntimeException("Account not found"));

        ServletException thrown = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "25.00")));

        assertThat(thrown.getRootCause())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account not found");
        verify(accountService, never()).transferAmount(any(), anyString(), any());
    }
}
