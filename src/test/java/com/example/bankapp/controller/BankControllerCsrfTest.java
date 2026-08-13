package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Control APP-SEC-05: state-changing endpoints reject requests without a valid CSRF token.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BankControllerCsrfTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    private String username;

    @BeforeEach
    void setUp() {
        username = "csrf-test-" + UUID.randomUUID();
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("{noop}irrelevant");
        account.setBalance(new BigDecimal("100.00"));
        accountRepository.save(account);
    }

    @Test
    void depositWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "10.00").with(user(username)))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "someone").param("amount", "10.00").with(user(username)))
                .andExpect(status().isForbidden());
    }

    @Test
    void depositWithCsrfTokenIsAccepted() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "10.00").with(user(username)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
