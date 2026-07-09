package com.example.bankapp.config;

import com.example.bankapp.controller.BankController;
import com.example.bankapp.model.Account;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CHARACTERIZATION TEST for CSRF protection configured in {@link SecurityConfig}.
 *
 * <p>Originally pinned the pre-remediation state (CSRF disabled: a state-changing
 * POST was accepted without a token). After remediation CSRF is enabled, so a
 * POST without a valid token is now rejected while a POST with a token succeeds.
 */
@WebMvcTest(controllers = BankController.class)
@Import(SecurityConfig.class)
class CsrfProtectionCharacterizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("REMEDIATED: a POST without a CSRF token is forbidden because CSRF is enabled")
    void deposit_withoutCsrfToken_isForbidden() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "50.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("a POST with a valid CSRF token is accepted")
    void deposit_withCsrfToken_accepted() throws Exception {
        Account account = new Account();
        account.setUsername("alice");
        account.setBalance(new BigDecimal("100.00"));
        when(accountService.findAccountByUsername("alice")).thenReturn(account);

        mockMvc.perform(post("/deposit").param("amount", "50.00").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
