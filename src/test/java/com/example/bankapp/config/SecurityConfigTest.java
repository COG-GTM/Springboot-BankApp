package com.example.bankapp.config;

import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        try {
            accountService.registerAccount("testuser", "password123");
        } catch (RuntimeException e) {
        }
    }

    @Test
    void testPublicEndpoint_Register_AccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/register"))
            .andExpect(status().isOk());
    }

    @Test
    void testPublicEndpoint_Login_AccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }

    @Test
    void testProtectedEndpoint_Dashboard_RedirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void testProtectedEndpoint_Transactions_RedirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/transactions"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testProtectedEndpoint_Dashboard_AccessibleWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testProtectedEndpoint_Transactions_AccessibleWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/transactions"))
            .andExpect(status().isOk());
    }

    @Test
    void testProtectedEndpoint_Deposit_RedirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/deposit")
                .param("amount", "100.00"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void testProtectedEndpoint_Withdraw_RedirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/withdraw")
                .param("amount", "50.00"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void testProtectedEndpoint_Transfer_RedirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/transfer")
                .param("toUsername", "recipient")
                .param("amount", "100.00"))
            .andExpect(status().is3xxRedirection());
    }
}
