package com.example.bankapp.config;

import com.example.bankapp.model.Account;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
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

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUsername("testuser");
        testAccount.setPassword("encodedPassword");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setTransactions(new ArrayList<>());
    }

    @Test
    void passwordEncoder_ReturnsBCryptPasswordEncoder() {
        assertNotNull(passwordEncoder);
        String encoded = passwordEncoder.encode("testPassword");
        assertTrue(passwordEncoder.matches("testPassword", encoded));
    }

    @Test
    void passwordEncoder_EncodesPasswordDifferentlyEachTime() {
        String encoded1 = passwordEncoder.encode("testPassword");
        String encoded2 = passwordEncoder.encode("testPassword");
        assertNotEquals(encoded1, encoded2);
    }

    @Test
    void registerEndpoint_IsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void loginEndpoint_IsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardEndpoint_RequiresAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void transactionsEndpoint_RequiresAuthentication() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void dashboardEndpoint_IsAccessibleWhenAuthenticated() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    void depositEndpoint_RequiresAuthentication() throws Exception {
        mockMvc.perform(post("/deposit")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void withdrawEndpoint_RequiresAuthentication() throws Exception {
        mockMvc.perform(post("/withdraw")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void transferEndpoint_RequiresAuthentication() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void logout_InvalidatesSessionAndRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void loginPage_HasCorrectUrl() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void registerPost_IsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "password123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
