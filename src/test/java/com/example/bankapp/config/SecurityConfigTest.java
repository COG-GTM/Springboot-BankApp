package com.example.bankapp.config;

import com.example.bankapp.model.Account;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AccountService accountService;

    // --- Public endpoint tests ---

    @Test
    void register_isAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void login_isAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    // --- Protected endpoint tests ---

    @Test
    void dashboard_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void transactions_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void deposit_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/deposit")
                        .param("amount", "100"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void withdraw_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/withdraw")
                        .param("amount", "100"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void transfer_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("toUsername", "user2")
                        .param("amount", "100"))
                .andExpect(status().is3xxRedirection());
    }

    // --- Authenticated access tests ---

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_accessibleWhenAuthenticated() throws Exception {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("1000.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void transactions_accessibleWhenAuthenticated() throws Exception {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        when(accountService.getTransactionHistory(account)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk());
    }

    // --- Logout tests ---

    @Test
    @WithMockUser
    void logout_redirectsToLoginWithParam() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    // --- Password encoder test ---

    @Test
    void passwordEncoder_isBCrypt() {
        assertNotNull(passwordEncoder);
        String encoded = passwordEncoder.encode("test");
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$") || encoded.startsWith("$2y$"));
    }

    private void assertNotNull(Object obj) {
        org.junit.jupiter.api.Assertions.assertNotNull(obj);
    }

    private void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
}
