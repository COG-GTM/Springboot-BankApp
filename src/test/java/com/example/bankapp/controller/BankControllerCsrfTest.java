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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BankController.class)
@Import(SecurityConfig.class)
class BankControllerCsrfTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    private final Account account = new Account();

    @BeforeEach
    void setUp() {
        account.setUsername("alice");
        account.setBalance(new BigDecimal("1000"));
        when(accountService.findAccountByUsername("alice")).thenReturn(account);
    }

    @Test
    @WithMockUser(username = "alice")
    void transferWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "mallory").param("amount", "500"))
                .andExpect(status().isForbidden());
        verify(accountService, never()).transferAmount(any(), anyString(), any());
    }

    @Test
    @WithMockUser(username = "alice")
    void transferWithCsrfTokenSucceeds() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "500").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
        verify(accountService).transferAmount(account, "bob", new BigDecimal("500"));
    }

    @Test
    @WithMockUser(username = "alice")
    void withdrawWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/withdraw").param("amount", "500"))
                .andExpect(status().isForbidden());
        verify(accountService, never()).withdraw(any(), any());
    }

    @Test
    @WithMockUser(username = "alice")
    void withdrawWithCsrfTokenSucceeds() throws Exception {
        mockMvc.perform(post("/withdraw").param("amount", "500").with(csrf()))
                .andExpect(redirectedUrl("/dashboard"));
        verify(accountService).withdraw(account, new BigDecimal("500"));
    }

    @Test
    @WithMockUser(username = "alice")
    void depositWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "500"))
                .andExpect(status().isForbidden());
        verify(accountService, never()).deposit(any(), any());
    }

    @Test
    @WithMockUser(username = "alice")
    void depositWithCsrfTokenSucceeds() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "500").with(csrf()))
                .andExpect(redirectedUrl("/dashboard"));
        verify(accountService).deposit(account, new BigDecimal("500"));
    }

    @Test
    @WithMockUser(username = "alice")
    void dashboardFormsCarryCsrfToken() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    @WithMockUser(username = "alice")
    void logoutViaGetIsNotHandled() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice")
    void logoutViaPostWithCsrfTokenSucceeds() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
    }
}
