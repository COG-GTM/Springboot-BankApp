package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankControllerTest {

    private static final String USERNAME = "alice";

    @Mock
    private AccountService accountService;

    @InjectMocks
    private BankController controller;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setUsername(USERNAME);
        account.setBalance(new BigDecimal("100"));
    }

    private void authenticateAs(String username) {
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new TestingAuthenticationToken(username, "password"));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dashboardAddsAccountToModelAndReturnsDashboardView() {
        authenticateAs(USERNAME);
        when(accountService.findAccountByUsername(USERNAME)).thenReturn(account);
        Model model = new ExtendedModelMap();

        String view = controller.dashboard(model);

        assertEquals("dashboard", view);
        assertSame(account, model.getAttribute("account"));
        verify(accountService).findAccountByUsername(USERNAME);
    }

    @Test
    void showRegistrationFormReturnsRegisterView() {
        assertEquals("register", controller.showRegistrationForm());
        verifyNoInteractions(accountService);
    }

    @Test
    void registerAccountOnSuccessRedirectsToLogin() {
        Model model = new ExtendedModelMap();

        String view = controller.registerAccount(USERNAME, "secret", model);

        assertEquals("redirect:/login", view);
        verify(accountService).registerAccount(USERNAME, "secret");
        assertEquals(null, model.getAttribute("error"));
    }

    @Test
    void registerAccountOnRuntimeExceptionAddsErrorAndReturnsRegisterView() {
        Model model = new ExtendedModelMap();
        when(accountService.registerAccount(USERNAME, "secret"))
                .thenThrow(new RuntimeException("Username already exists"));

        String view = controller.registerAccount(USERNAME, "secret", model);

        assertEquals("register", view);
        assertEquals("Username already exists", model.getAttribute("error"));
        verify(accountService).registerAccount(USERNAME, "secret");
    }

    @Test
    void loginReturnsLoginView() {
        assertEquals("login", controller.login());
        verifyNoInteractions(accountService);
    }

    @Test
    void depositCallsServiceAndRedirectsToDashboard() {
        authenticateAs(USERNAME);
        BigDecimal amount = new BigDecimal("50");
        when(accountService.findAccountByUsername(USERNAME)).thenReturn(account);

        String view = controller.deposit(amount);

        assertEquals("redirect:/dashboard", view);
        verify(accountService).findAccountByUsername(USERNAME);
        verify(accountService).deposit(account, amount);
    }

    @Test
    void withdrawOnSuccessRedirectsToDashboard() {
        authenticateAs(USERNAME);
        BigDecimal amount = new BigDecimal("50");
        when(accountService.findAccountByUsername(USERNAME)).thenReturn(account);
        Model model = new ExtendedModelMap();

        String view = controller.withdraw(amount, model);

        assertEquals("redirect:/dashboard", view);
        verify(accountService).withdraw(account, amount);
        assertEquals(null, model.getAttribute("error"));
        assertEquals(null, model.getAttribute("account"));
    }

    @Test
    void withdrawOnRuntimeExceptionAddsErrorAndAccountAndReturnsDashboardView() {
        authenticateAs(USERNAME);
        BigDecimal amount = new BigDecimal("500");
        when(accountService.findAccountByUsername(USERNAME)).thenReturn(account);
        org.mockito.Mockito.doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(account, amount);
        Model model = new ExtendedModelMap();

        String view = controller.withdraw(amount, model);

        assertEquals("dashboard", view);
        assertEquals("Insufficient funds", model.getAttribute("error"));
        assertSame(account, model.getAttribute("account"));
        verify(accountService).withdraw(account, amount);
    }

    @Test
    void transactionHistoryAddsTransactionsToModelAndReturnsTransactionsView() {
        authenticateAs(USERNAME);
        when(accountService.findAccountByUsername(USERNAME)).thenReturn(account);
        List<Transaction> transactions = Arrays.asList(
                new Transaction(new BigDecimal("50"), "Deposit", LocalDateTime.now(), account),
                new Transaction(new BigDecimal("20"), "Withdrawal", LocalDateTime.now(), account)
        );
        when(accountService.getTransactionHistory(account)).thenReturn(transactions);
        Model model = new ExtendedModelMap();

        String view = controller.transactionHistory(model);

        assertEquals("transactions", view);
        assertSame(transactions, model.getAttribute("transactions"));
        verify(accountService).getTransactionHistory(account);
    }

    @Test
    void transferAmountOnSuccessRedirectsToDashboard() {
        authenticateAs(USERNAME);
        BigDecimal amount = new BigDecimal("25");
        when(accountService.findAccountByUsername(USERNAME)).thenReturn(account);
        Model model = new ExtendedModelMap();

        String view = controller.transferAmount("bob", amount, model);

        assertEquals("redirect:/dashboard", view);
        verify(accountService).transferAmount(account, "bob", amount);
        assertEquals(null, model.getAttribute("error"));
        assertEquals(null, model.getAttribute("account"));
    }

    @Test
    void transferAmountOnRuntimeExceptionAddsErrorAndAccountAndReturnsDashboardView() {
        authenticateAs(USERNAME);
        BigDecimal amount = new BigDecimal("1000");
        when(accountService.findAccountByUsername(USERNAME)).thenReturn(account);
        org.mockito.Mockito.doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).transferAmount(account, "bob", amount);
        Model model = new ExtendedModelMap();

        String view = controller.transferAmount("bob", amount, model);

        assertEquals("dashboard", view);
        assertEquals("Insufficient funds", model.getAttribute("error"));
        assertSame(account, model.getAttribute("account"));
        verify(accountService).transferAmount(account, "bob", amount);
        verify(accountService, times(1)).findAccountByUsername(USERNAME);
    }

    @Test
    void registerAccountDoesNotResolveSecurityContext() {
        Model model = new ExtendedModelMap();

        controller.registerAccount(USERNAME, "secret", model);

        verify(accountService, never()).findAccountByUsername(USERNAME);
    }
}
