package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankController.class)
@AutoConfigureMockMvc(addFilters = false)
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Test
    void getLogin_returns200AndLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void getRegister_returns200AndRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void postRegister_success_redirectsToLogin() throws Exception {
        when(accountService.registerAccount("newuser", "pass123")).thenReturn(new Account());

        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "pass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void postRegister_failure_returnsRegisterViewWithError() throws Exception {
        when(accountService.registerAccount("existing", "pass123"))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                        .param("username", "existing")
                        .param("password", "pass123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getDashboard_returnsAccountAndDashboardView() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setBalance(new BigDecimal("100.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void postDeposit_redirectsToDashboard() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/deposit")
                        .param("amount", "50.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).deposit(any(Account.class), eq(new BigDecimal("50.00")));
    }

    @Test
    @WithMockUser(username = "testuser")
    void postWithdraw_success_redirectsToDashboard() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/withdraw")
                        .param("amount", "25.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void postWithdraw_failure_returnsDashboardWithError() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(any(Account.class), any(BigDecimal.class));

        mockMvc.perform(post("/withdraw")
                        .param("amount", "1000.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getTransactions_returnsTransactionsView() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        account.setId(1L);
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        when(accountService.getTransactionHistory(any(Account.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attributeExists("transactions"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void postTransfer_success_redirectsToDashboard() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "50.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void postTransfer_failure_returnsDashboardWithError() throws Exception {
        Account account = new Account();
        account.setUsername("testuser");
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).transferAmount(any(Account.class), anyString(), any(BigDecimal.class));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "1000.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }
}
