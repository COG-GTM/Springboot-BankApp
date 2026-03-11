package com.example.bankapp.config;

import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private AccountRepository accountRepository;

    @Test
    void registerPage_shouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void loginPage_shouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void dashboard_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void deposit_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/deposit"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void withdraw_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/withdraw"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void transfer_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/transfer"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void transactions_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().is3xxRedirection());
    }
}
