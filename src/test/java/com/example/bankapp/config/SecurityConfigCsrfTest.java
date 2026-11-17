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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BankController.class)
@Import(SecurityConfig.class)
class SecurityConfigCsrfTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Test
    @WithMockUser(username = "alice")
    void postDeposit_withoutCsrfToken_isForbidden() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice")
    void postTransfer_withoutCsrfToken_isForbidden() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("toUsername", "bob")
                        .param("amount", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice")
    void postDeposit_withCsrfToken_isAccepted() throws Exception {
        when(accountService.findAccountByUsername(anyString())).thenReturn(new Account());
        mockMvc.perform(post("/deposit").param("amount", "10").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "alice")
    void postTransfer_withCsrfToken_isAccepted() throws Exception {
        Account from = new Account();
        from.setBalance(new BigDecimal("100"));
        when(accountService.findAccountByUsername(anyString())).thenReturn(from);
        mockMvc.perform(post("/transfer")
                        .param("toUsername", "bob")
                        .param("amount", "10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
