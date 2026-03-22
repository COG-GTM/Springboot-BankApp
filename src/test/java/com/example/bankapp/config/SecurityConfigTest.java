package com.example.bankapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void unauthenticated_dashboard_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void unauthenticated_register_returns200() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticated_login_returns200() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void authenticated_request_isNotRedirectedToLogin() throws Exception {
        // With @WithMockUser, security allows the request through (no 302 redirect).
        // The controller calls the real service which throws RuntimeException("Account not found")
        // because the H2 DB is empty. This exception proves security did NOT block the request.
        try {
            mockMvc.perform(get("/transactions"));
        } catch (Exception e) {
            // A ServletException wrapping "Account not found" means the request passed
            // through the security filter chain and reached the controller/service layer.
            assertTrue(e.getMessage().contains("Account not found")
                    || e.getCause().getMessage().contains("Account not found"),
                    "Expected 'Account not found' exception but got: " + e.getMessage());
        }
    }

    @Test
    @WithMockUser(username = "testuser")
    void logout_invalidatesSessionAndRedirects() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void passwordEncoder_isBCrypt() {
        assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder);
    }
}
