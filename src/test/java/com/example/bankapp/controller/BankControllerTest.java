package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankController.class)
@Import(com.example.bankapp.config.TestSecurityConfig.class)
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    private Account testAccount;
    private List<Transaction> testTransactions;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUsername("testuser");
        testAccount.setPassword("password");
        testAccount.setBalance(new BigDecimal("1000.00"));

        Transaction transaction1 = new Transaction();
        transaction1.setId(1L);
        transaction1.setAmount(new BigDecimal("500.00"));
        transaction1.setType("Deposit");
        transaction1.setTimestamp(LocalDateTime.now());
        transaction1.setAccount(testAccount);

        Transaction transaction2 = new Transaction();
        transaction2.setId(2L);
        transaction2.setAmount(new BigDecimal("100.00"));
        transaction2.setType("Withdrawal");
        transaction2.setTimestamp(LocalDateTime.now());
        transaction2.setAccount(testAccount);

        testTransactions = Arrays.asList(transaction1, transaction2);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDashboard_ShouldReturnDashboardView() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("account", testAccount));

        verify(accountService).findAccountByUsername("testuser");
    }

    @Test
    void testShowRegistrationForm_ShouldReturnRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void testLogin_ShouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void testRegisterAccount_Success_ShouldRedirectToLogin() throws Exception {
        when(accountService.registerAccount("newuser", "password")).thenReturn(testAccount);

        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(accountService).registerAccount("newuser", "password");
    }

    @Test
    void testRegisterAccount_Failure_ShouldReturnRegisterViewWithError() throws Exception {
        when(accountService.registerAccount("existinguser", "password"))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                        .param("username", "existinguser")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Username already exists"));

        verify(accountService).registerAccount("existinguser", "password");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeposit_ShouldRedirectToDashboard() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);

        mockMvc.perform(post("/deposit")
                        .param("amount", "250.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).findAccountByUsername("testuser");
        verify(accountService).deposit(testAccount, new BigDecimal("250.00"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testWithdraw_Success_ShouldRedirectToDashboard() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);

        mockMvc.perform(post("/withdraw")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).findAccountByUsername("testuser");
        verify(accountService).withdraw(testAccount, new BigDecimal("100.00"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testWithdraw_InsufficientFunds_ShouldReturnDashboardWithError() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(testAccount, new BigDecimal("2000.00"));

        mockMvc.perform(post("/withdraw")
                        .param("amount", "2000.00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Insufficient funds"))
                .andExpect(model().attribute("account", testAccount));

        verify(accountService).findAccountByUsername("testuser");
        verify(accountService).withdraw(testAccount, new BigDecimal("2000.00"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testTransactionHistory_ShouldReturnTransactionsView() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        when(accountService.getTransactionHistory(testAccount)).thenReturn(testTransactions);

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attribute("transactions", testTransactions));

        verify(accountService).findAccountByUsername("testuser");
        verify(accountService).getTransactionHistory(testAccount);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testTransferAmount_Success_ShouldRedirectToDashboard() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "200.00")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(accountService).findAccountByUsername("testuser");
        verify(accountService).transferAmount(testAccount, "recipient", new BigDecimal("200.00"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testTransferAmount_InsufficientFunds_ShouldReturnDashboardWithError() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).transferAmount(testAccount, "recipient", new BigDecimal("2000.00"));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "recipient")
                        .param("amount", "2000.00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Insufficient funds"))
                .andExpect(model().attribute("account", testAccount));

        verify(accountService).findAccountByUsername("testuser");
        verify(accountService).transferAmount(testAccount, "recipient", new BigDecimal("2000.00"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testTransferAmount_RecipientNotFound_ShouldReturnDashboardWithError() throws Exception {
        when(accountService.findAccountByUsername("testuser")).thenReturn(testAccount);
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(testAccount, "nonexistent", new BigDecimal("100.00"));

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "nonexistent")
                        .param("amount", "100.00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Recipient account not found"))
                .andExpect(model().attribute("account", testAccount));

        verify(accountService).findAccountByUsername("testuser");
        verify(accountService).transferAmount(testAccount, "nonexistent", new BigDecimal("100.00"));
    }
}
