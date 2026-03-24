package com.example.bankapp.config;

import com.example.bankapp.model.Account;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

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

    @MockBean
    private AccountService accountService;

    // --- Public endpoints ---

    @Test
    void registerPage_IsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void loginPage_IsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    // --- Protected endpoints redirect to login ---

    @Test
    void dashboard_RedirectsToLogin_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void transactions_RedirectsToLogin_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void deposit_RedirectsToLogin_WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/deposit")
                        .param("amount", "100")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void withdraw_RedirectsToLogin_WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/withdraw")
                        .param("amount", "100")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void transfer_RedirectsToLogin_WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("toUsername", "user2")
                        .param("amount", "100")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // --- Authenticated access ---

    @Test
    @WithMockUser(username = "testuser")
    void dashboard_IsAccessible_WhenAuthenticated() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("100.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void transactions_IsAccessible_WhenAuthenticated() throws Exception {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("testuser");
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        when(accountService.getTransactionHistory(account)).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk());
    }

    // --- Logout ---

    @Test
    @WithMockUser(username = "testuser")
    void logout_RedirectsToLoginWithLogoutParam() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    // --- Post registration ---

    @Test
    void registerPost_IsAccessibleWithoutAuthentication() throws Exception {
        Account account = new Account();
        account.setUsername("newuser");
        when(accountService.registerAccount("newuser", "password")).thenReturn(account);

        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
