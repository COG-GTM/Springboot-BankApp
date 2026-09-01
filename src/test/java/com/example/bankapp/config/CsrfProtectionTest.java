package com.example.bankapp.config;

import com.example.bankapp.controller.BankController;
import com.example.bankapp.model.Account;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BankController.class)
@Import(SecurityConfig.class)
class CsrfProtectionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Test
    @WithMockUser(username = "victim")
    void transferWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("toUsername", "attacker")
                        .param("amount", "100"))
                .andExpect(status().isForbidden());

        verify(accountService, never()).transferAmount(any(), anyString(), any());
    }

    @Test
    @WithMockUser(username = "victim")
    void transferWithCsrfTokenIsAccepted() throws Exception {
        Account account = new Account();
        account.setUsername("victim");
        account.setBalance(new BigDecimal("500"));
        when(accountService.findAccountByUsername("victim")).thenReturn(account);

        mockMvc.perform(post("/transfer").with(csrf())
                        .param("toUsername", "attacker")
                        .param("amount", "100"))
                .andExpect(status().is3xxRedirection());

        verify(accountService).transferAmount(account, "attacker", new BigDecimal("100"));
    }

    @Test
    @WithMockUser(username = "victim")
    void depositAndWithdrawWithoutCsrfTokenAreForbidden() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "100"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/withdraw").param("amount", "100"))
                .andExpect(status().isForbidden());

        verify(accountService, never()).deposit(any(), any());
        verify(accountService, never()).withdraw(any(), any());
    }

    @Test
    void registerWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "attacker")
                        .param("password", "secret"))
                .andExpect(status().isForbidden());

        verify(accountService, never()).registerAccount(anyString(), anyString());
    }
}
