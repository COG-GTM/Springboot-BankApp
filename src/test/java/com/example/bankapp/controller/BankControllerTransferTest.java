package com.example.bankapp.controller;

import com.example.bankapp.config.SecurityConfig;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(BankController.class)
@Import(SecurityConfig.class)
class BankControllerTransferTest {

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

    private static BigDecimal amountEq(String expected) {
        BigDecimal value = new BigDecimal(expected);
        return argThat(actual -> actual != null && actual.compareTo(value) == 0);
    }

    @Test
    @WithMockUser(username = "alice")
    void transfer_success_redirectsToDashboardAndPassesParamsThrough() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "25.00"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));

        ArgumentCaptor<Account> fromCaptor = ArgumentCaptor.forClass(Account.class);
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(accountService).findAccountByUsername("alice");
        verify(accountService).transferAmount(fromCaptor.capture(), eq("bob"), amountCaptor.capture());
        assertThat(fromCaptor.getValue()).isSameAs(alice);
        assertThat(amountCaptor.getValue()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    @WithMockUser(username = "alice")
    void transfer_insufficientFunds_rendersDashboardWithError() throws Exception {
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).transferAmount(same(alice), eq("bob"), amountEq("500"));

        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "500"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Insufficient funds"))
                .andExpect(model().attribute("account", sameInstance(alice)))
                .andExpect(content().string(containsString("Insufficient funds")))
                .andExpect(content().string(containsString("Welcome, alice")));
    }

    @Test
    @WithMockUser(username = "alice")
    void transfer_recipientNotFound_rendersDashboardWithError() throws Exception {
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(same(alice), eq("ghost"), amountEq("10"));

        mockMvc.perform(post("/transfer").param("toUsername", "ghost").param("amount", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Recipient account not found"))
                .andExpect(model().attribute("account", sameInstance(alice)))
                .andExpect(content().string(containsString("Recipient account not found")));
    }

    @Test
    void transfer_unauthenticated_redirectsToLoginAndNeverTransfers() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "25.00"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/login"));

        verify(accountService, never()).transferAmount(any(), anyString(), any());
    }

    @Test
    void transfer_withoutCsrfToken_succeedsForAuthenticatedUser() throws Exception {
        // LIKELY BUG: CSRF disabled on money-moving endpoint
        mockMvc.perform(post("/transfer").with(user("alice"))
                        .param("toUsername", "bob").param("amount", "25.00"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(same(alice), eq("bob"), amountEq("25.00"));
    }

    @Test
    @WithMockUser(username = "alice")
    void transfer_missingAmount_returns400AndNeverCallsService() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @Test
    @WithMockUser(username = "alice")
    void transfer_missingToUsername_returns400AndNeverCallsService() throws Exception {
        mockMvc.perform(post("/transfer").param("amount", "25.00"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @Test
    @WithMockUser(username = "alice")
    void transfer_nonNumericAmount_returns400AndNeverCallsService() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @Test
    @WithMockUser(username = "alice")
    void transfer_negativeAmount_isPassedStraightToService() throws Exception {
        // LIKELY BUG: no web-layer validation; a negative transfer reaches the service and redirects as success
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "-50"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(same(alice), eq("bob"), amountEq("-50"));
    }

    @Test
    @WithMockUser(username = "alice")
    void transfer_zeroAmount_isPassedStraightToService() throws Exception {
        // LIKELY BUG: no web-layer validation; a zero transfer reaches the service and redirects as success
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "0"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(same(alice), eq("bob"), amountEq("0"));
    }

    @Test
    @WithMockUser(username = "alice")
    void transfer_toSelf_isPassedStraightToService() throws Exception {
        // LIKELY BUG: web layer does not reject self-transfer (toUsername equals the authenticated user)
        mockMvc.perform(post("/transfer").param("toUsername", "alice").param("amount", "10"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(same(alice), eq("alice"), amountEq("10"));
    }

    @Test
    @WithMockUser(username = "alice")
    void transfer_senderAccountLookupThrows_exceptionPropagatesOutsideTryBlock() throws Exception {
        // LIKELY BUG: findAccountByUsername is outside the try block, so a vanished account surfaces as an unhandled ServletException (500) instead of a dashboard error
        when(accountService.findAccountByUsername("alice")).thenThrow(new RuntimeException("Account not found"));

        assertThatThrownBy(() -> mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "10")))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("Account not found");

        verify(accountService, never()).transferAmount(any(), anyString(), any());
    }

    @Test
    @WithMockUser(username = "alice")
    void transactions_authenticated_rendersHistoryWithDirectionStyling() throws Exception {
        Transaction out = new Transaction(new BigDecimal("25.00"), "Transfer Out to bob", LocalDateTime.of(2026, 1, 2, 10, 0), alice);
        out.setId(11L);
        Transaction in = new Transaction(new BigDecimal("40.00"), "Transfer In from bob", LocalDateTime.of(2026, 1, 3, 11, 0), alice);
        in.setId(12L);
        List<Transaction> history = List.of(out, in);
        when(accountService.getTransactionHistory(alice)).thenReturn(history);

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attribute("transactions", history))
                .andExpect(content().string(containsString("Transfer Out to bob")))
                .andExpect(content().string(containsString("Transfer In from bob")))
                .andExpect(content().string(containsString("<td class=\"text-danger\">25.00</td>")))
                .andExpect(content().string(containsString("<td class=\"text-success\">40.00</td>")))
                // LIKELY BUG: th:text on the amount <td> replaces the inner <span>, so the "+$"/"-$" prefix is never rendered
                .andExpect(content().string(not(containsString("-$25.00"))))
                .andExpect(content().string(not(containsString("+$40.00"))));

        verify(accountService).getTransactionHistory(same(alice));
    }

    @Test
    void transactions_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/login"));

        verify(accountService, never()).getTransactionHistory(any());
    }
}
