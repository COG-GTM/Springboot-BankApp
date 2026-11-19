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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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

    private Account alice;

    @BeforeEach
    void setUp() {
        alice = new Account();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setBalance(new BigDecimal("1000.00"));
        when(accountService.findAccountByUsername("alice")).thenReturn(alice);
    }

    @Test
    void transferWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/transfer").with(user("alice"))
                        .param("toUsername", "mallory").param("amount", "5000"))
                .andExpect(status().isForbidden());
        verify(accountService, never()).transferAmount(any(), anyString(), any());
    }

    @Test
    void transferWithInvalidCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/transfer").with(user("alice")).with(csrf().useInvalidToken())
                        .param("toUsername", "mallory").param("amount", "5000"))
                .andExpect(status().isForbidden());
        verify(accountService, never()).transferAmount(any(), anyString(), any());
    }

    @Test
    void transferWithCsrfTokenSucceeds() throws Exception {
        mockMvc.perform(post("/transfer").with(user("alice")).with(csrf())
                        .param("toUsername", "bob").param("amount", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
        verify(accountService).transferAmount(alice, "bob", new BigDecimal("50"));
    }

    @Test
    void withdrawWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/withdraw").with(user("alice")).param("amount", "100"))
                .andExpect(status().isForbidden());
        verify(accountService, never()).withdraw(any(), any());
    }

    @Test
    void withdrawWithCsrfTokenSucceeds() throws Exception {
        mockMvc.perform(post("/withdraw").with(user("alice")).with(csrf()).param("amount", "100"))
                .andExpect(redirectedUrl("/dashboard"));
        verify(accountService).withdraw(alice, new BigDecimal("100"));
    }

    @Test
    void depositWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/deposit").with(user("alice")).param("amount", "100"))
                .andExpect(status().isForbidden());
        verify(accountService, never()).deposit(any(), any());
    }

    @Test
    void depositWithCsrfTokenSucceeds() throws Exception {
        mockMvc.perform(post("/deposit").with(user("alice")).with(csrf()).param("amount", "100"))
                .andExpect(redirectedUrl("/dashboard"));
        verify(accountService).deposit(alice, new BigDecimal("100"));
    }

    @Test
    void registerWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/register").param("username", "eve").param("password", "pw"))
                .andExpect(status().isForbidden());
        verify(accountService, never()).registerAccount(anyString(), anyString());
    }

    @Test
    void logoutRequiresPostWithCsrfToken() throws Exception {
        mockMvc.perform(get("/logout").with(user("alice")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/logout").with(user("alice")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/logout").with(user("alice")).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void dashboardFormsCarryCsrfToken() throws Exception {
        String body = mockMvc.perform(get("/dashboard").with(user("alice")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int tokens = body.split("name=\"_csrf\"", -1).length - 1;
        // deposit, withdraw, transfer, logout
        assertEquals(4, tokens, body);
    }

    @Test
    void loginAndRegisterFormsCarryCsrfToken() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }
}
