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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankController.class)
@AutoConfigureMockMvc(addFilters = false)
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    private Account createTestAccount(String username, BigDecimal balance) {
        Account account = new Account();
        account.setId(1L);
        account.setUsername(username);
        account.setPassword("encodedPassword");
        account.setBalance(balance);
        return account;
    }

    @Test
    void testShowRegistrationForm_ReturnsRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(view().name("register"));
    }

    @Test
    void testRegisterAccount_Success_RedirectsToLogin() throws Exception {
        Account newAccount = createTestAccount("newuser", BigDecimal.ZERO);
        when(accountService.registerAccount("newuser", "password123"))
            .thenReturn(newAccount);

        mockMvc.perform(post("/register")
                .param("username", "newuser")
                .param("password", "password123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        verify(accountService).registerAccount("newuser", "password123");
    }

    @Test
    void testRegisterAccount_UsernameExists_ReturnsRegisterViewWithError() throws Exception {
        when(accountService.registerAccount("existinguser", "password123"))
            .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                .param("username", "existinguser")
                .param("password", "password123"))
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(model().attributeExists("error"))
            .andExpect(model().attribute("error", "Username already exists"));
    }

    @Test
    void testLogin_ReturnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("login"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDashboard_AuthenticatedUser_ReturnsDashboardView() throws Exception {
        Account account = createTestAccount("testuser", new BigDecimal("1000.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);

        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard"))
            .andExpect(model().attributeExists("account"))
            .andExpect(model().attribute("account", account));

        verify(accountService).findAccountByUsername("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeposit_Success_RedirectsToDashboard() throws Exception {
        Account account = createTestAccount("testuser", new BigDecimal("1000.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doNothing().when(accountService).deposit(account, new BigDecimal("100.00"));

        mockMvc.perform(post("/deposit")
                .param("amount", "100.00"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).findAccountByUsername("testuser");
        verify(accountService).deposit(account, new BigDecimal("100.00"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testWithdraw_Success_RedirectsToDashboard() throws Exception {
        Account account = createTestAccount("testuser", new BigDecimal("1000.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doNothing().when(accountService).withdraw(account, new BigDecimal("50.00"));

        mockMvc.perform(post("/withdraw")
                .param("amount", "50.00"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).withdraw(account, new BigDecimal("50.00"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testWithdraw_InsufficientFunds_ReturnsDashboardViewWithError() throws Exception {
        Account account = createTestAccount("testuser", new BigDecimal("50.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds"))
            .when(accountService).withdraw(account, new BigDecimal("100.00"));

        mockMvc.perform(post("/withdraw")
                .param("amount", "100.00"))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard"))
            .andExpect(model().attributeExists("error"))
            .andExpect(model().attribute("error", "Insufficient funds"))
            .andExpect(model().attributeExists("account"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testTransactionHistory_ReturnsTransactionsView() throws Exception {
        Account account = createTestAccount("testuser", new BigDecimal("1000.00"));
        List<Transaction> transactions = Arrays.asList(
            new Transaction(new BigDecimal("100.00"), "Deposit", LocalDateTime.now(), account),
            new Transaction(new BigDecimal("50.00"), "Withdrawal", LocalDateTime.now(), account)
        );

        when(accountService.findAccountByUsername("testuser")).thenReturn(account);
        when(accountService.getTransactionHistory(account)).thenReturn(transactions);

        mockMvc.perform(get("/transactions"))
            .andExpect(status().isOk())
            .andExpect(view().name("transactions"))
            .andExpect(model().attributeExists("transactions"))
            .andExpect(model().attribute("transactions", transactions));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testTransfer_Success_RedirectsToDashboard() throws Exception {
        Account fromAccount = createTestAccount("testuser", new BigDecimal("1000.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(fromAccount);
        doNothing().when(accountService).transferAmount(fromAccount, "recipient", new BigDecimal("200.00"));

        mockMvc.perform(post("/transfer")
                .param("toUsername", "recipient")
                .param("amount", "200.00"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).transferAmount(fromAccount, "recipient", new BigDecimal("200.00"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testTransfer_InsufficientFunds_ReturnsDashboardViewWithError() throws Exception {
        Account fromAccount = createTestAccount("testuser", new BigDecimal("50.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(fromAccount);
        doThrow(new RuntimeException("Insufficient funds"))
            .when(accountService).transferAmount(fromAccount, "recipient", new BigDecimal("200.00"));

        mockMvc.perform(post("/transfer")
                .param("toUsername", "recipient")
                .param("amount", "200.00"))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard"))
            .andExpect(model().attributeExists("error"))
            .andExpect(model().attribute("error", "Insufficient funds"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testTransfer_RecipientNotFound_ReturnsDashboardViewWithError() throws Exception {
        Account fromAccount = createTestAccount("testuser", new BigDecimal("1000.00"));
        when(accountService.findAccountByUsername("testuser")).thenReturn(fromAccount);
        doThrow(new RuntimeException("Recipient account not found"))
            .when(accountService).transferAmount(fromAccount, "nonexistent", new BigDecimal("100.00"));

        mockMvc.perform(post("/transfer")
                .param("toUsername", "nonexistent")
                .param("amount", "100.00"))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard"))
            .andExpect(model().attributeExists("error"))
            .andExpect(model().attribute("error", "Recipient account not found"));
    }
}
