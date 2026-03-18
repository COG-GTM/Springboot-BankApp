package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private Model model;

    @InjectMocks
    private BankController bankController;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUpSecurityContext() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, TEST_PASSWORD, Collections.emptyList());
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Account createTestAccount() {
        Account account = new Account();
        account.setId(1L);
        account.setUsername(TEST_USERNAME);
        account.setPassword(TEST_PASSWORD);
        account.setBalance(new BigDecimal("1000.00"));
        return account;
    }

    // ---------------------------------------------------------------
    // GET /register
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("GET /register")
    class ShowRegistrationForm {

        @Test
        @DisplayName("returns register view name")
        void returnsRegisterView() {
            String view = bankController.showRegistrationForm();
            assertThat(view).isEqualTo("register");
        }
    }

    // ---------------------------------------------------------------
    // POST /register
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("POST /register")
    class RegisterAccount {

        @Test
        @DisplayName("successful registration redirects to login")
        void successfulRegistrationRedirectsToLogin() {
            Account account = createTestAccount();
            when(accountService.registerAccount(TEST_USERNAME, TEST_PASSWORD)).thenReturn(account);

            String view = bankController.registerAccount(TEST_USERNAME, TEST_PASSWORD, model);

            assertThat(view).isEqualTo("redirect:/login");
            verify(accountService, times(1)).registerAccount(TEST_USERNAME, TEST_PASSWORD);
            verifyNoInteractions(model);
        }

        @Test
        @DisplayName("duplicate username returns register view with error")
        void duplicateUsernameReturnsRegisterWithError() {
            String errorMessage = "Username already exists";
            when(accountService.registerAccount(TEST_USERNAME, TEST_PASSWORD))
                    .thenThrow(new RuntimeException(errorMessage));

            String view = bankController.registerAccount(TEST_USERNAME, TEST_PASSWORD, model);

            assertThat(view).isEqualTo("register");
            verify(model).addAttribute("error", errorMessage);
        }

        @Test
        @DisplayName("generic runtime exception during registration is handled")
        void genericRuntimeExceptionHandled() {
            String errorMessage = "Unexpected error";
            when(accountService.registerAccount("newuser", "pass"))
                    .thenThrow(new RuntimeException(errorMessage));

            String view = bankController.registerAccount("newuser", "pass", model);

            assertThat(view).isEqualTo("register");
            verify(model).addAttribute("error", errorMessage);
        }
    }

    // ---------------------------------------------------------------
    // GET /login
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("GET /login")
    class Login {

        @Test
        @DisplayName("returns login view name")
        void returnsLoginView() {
            String view = bankController.login();
            assertThat(view).isEqualTo("login");
        }
    }

    // ---------------------------------------------------------------
    // GET /dashboard
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("GET /dashboard")
    class Dashboard {

        @Test
        @DisplayName("populates model with account and returns dashboard view")
        void populatesModelAndReturnsDashboardView() {
            Account account = createTestAccount();
            when(accountService.findAccountByUsername(TEST_USERNAME)).thenReturn(account);

            String view = bankController.dashboard(model);

            assertThat(view).isEqualTo("dashboard");
            verify(model).addAttribute("account", account);
            verify(accountService).findAccountByUsername(TEST_USERNAME);
        }
    }

    // ---------------------------------------------------------------
    // POST /deposit
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("POST /deposit")
    class Deposit {

        @Test
        @DisplayName("successful deposit redirects to dashboard")
        void successfulDepositRedirectsToDashboard() {
            Account account = createTestAccount();
            BigDecimal amount = new BigDecimal("500.00");
            when(accountService.findAccountByUsername(TEST_USERNAME)).thenReturn(account);

            String view = bankController.deposit(amount);

            assertThat(view).isEqualTo("redirect:/dashboard");
            verify(accountService).findAccountByUsername(TEST_USERNAME);
            verify(accountService).deposit(account, amount);
        }

        @Test
        @DisplayName("deposit with zero amount still calls service")
        void depositZeroAmount() {
            Account account = createTestAccount();
            BigDecimal amount = BigDecimal.ZERO;
            when(accountService.findAccountByUsername(TEST_USERNAME)).thenReturn(account);

            String view = bankController.deposit(amount);

            assertThat(view).isEqualTo("redirect:/dashboard");
            verify(accountService).deposit(account, amount);
        }

        @Test
        @DisplayName("deposit with large amount succeeds")
        void depositLargeAmount() {
            Account account = createTestAccount();
            BigDecimal amount = new BigDecimal("999999999.99");
            when(accountService.findAccountByUsername(TEST_USERNAME)).thenReturn(account);

            String view = bankController.deposit(amount);

            assertThat(view).isEqualTo("redirect:/dashboard");
            verify(accountService).deposit(account, amount);
        }
    }

    // ---------------------------------------------------------------
    // POST /withdraw
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("POST /withdraw")
    class Withdraw {

        @Test
        @DisplayName("successful withdrawal redirects to dashboard")
        void successfulWithdrawalRedirectsToDashboard() {
            Account account = createTestAccount();
            BigDecimal amount = new BigDecimal("100.00");
            when(accountService.findAccountByUsername(TEST_USERNAME)).thenReturn(account);

            String view = bankController.withdraw(amount, model);

            assertThat(view).isEqualTo("redirect:/dashboard");
            verify(accountService).findAccountByUsername(TEST_USERNAME);
            verify(accountService).withdraw(account, amount);
            verifyNoInteractions(model);
        }

        @Test
        @DisplayName("insufficient funds returns dashboard with error")
        void insufficientFundsReturnsDashboardWithError() {
            Account account = createTestAccount();
            BigDecimal amount = new BigDecimal("5000.00");
            String errorMessage = "Insufficient funds";
            when(accountService.findAccountByUsername(TEST_USERNAME)).thenReturn(account);
            doThrow(new RuntimeException(errorMessage)).when(accountService).withdraw(account, amount);

            String view = bankController.withdraw(amount, model);

            assertThat(view).isEqualTo("dashboard");
            verify(model).addAttribute("error", errorMessage);
            verify(model).addAttribute("account", account);
        }

        @Test
        @DisplayName("generic runtime exception during withdrawal shows error on dashboard")
        void genericExceptionDuringWithdrawal() {
            Account account = createTestAccount();
            BigDecimal amount = new BigDecimal("100.00");
            String errorMessage = "Service unavailable";
            when(accountService.findAccountByUsername(TEST_USERNAME)).thenReturn(account);
            doThrow(new RuntimeException(errorMessage)).when(accountService).withdraw(account, amount);

            String view = bankController.withdraw(amount, model);

            assertThat(view).isEqualTo("dashboard");
            verify(model).addAttribute("error", errorMessage);
            verify(model).addAttribute("account", account);
        }
    }

    // ---------------------------------------------------------------
    // GET /transactions
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("GET /transactions")
    class TransactionHistory {

        @Test
        @DisplayName("populates model with transactions and returns transactions view")
        void populatesModelAndReturnsTransactionsView() {
            Account account = createTestAccount();
            Transaction tx1 = new Transaction(new BigDecimal("100.00"), "Deposit", LocalDateTime.now(), account);
            Transaction tx2 = new Transaction(new BigDecimal("50.00"), "Withdrawal", LocalDateTime.now(), account);
            List<Transaction> transactions = List.of(tx1, tx2);
            when(accountService.findAccountByUsername(TEST_USERNAME)).thenReturn(account);
            when(accountService.getTransactionHistory(account)).thenReturn(transactions);

            String view = bankController.transactionHistory(model);

            assertThat(view).isEqualTo("transactions");
            verify(model).addAttribute("transactions", transactions);
            verify(accountService).findAccountByUsername(TEST_USERNAME);
            verify(accountService).getTransactionHistory(account);
        }

        @Test
        @DisplayName("empty transaction history returns empty list in model")
        void emptyTransactionHistory() {
            Account account = createTestAccount();
            when(accountService.findAccountByUsername(TEST_USERNAME)).thenReturn(account);
            when(accountService.getTransactionHistory(account)).thenReturn(Collections.emptyList());

            String view = bankController.transactionHistory(model);

            assertThat(view).isEqualTo("transactions");
            verify(model).addAttribute("transactions", Collections.emptyList());
        }
    }

    // ---------------------------------------------------------------
    // POST /transfer
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("POST /transfer")
    class TransferAmount {

        @Test
        @DisplayName("successful transfer redirects to dashboard")
        void successfulTransferRedirectsToDashboard() {
            Account fromAccount = createTestAccount();
            String toUsername = "recipient";
            BigDecimal amount = new BigDecimal("200.00");
            when(accountService.findAccountByUsername(TEST_USERNAME)).thenReturn(fromAccount);

            String view = bankController.transferAmount(toUsername, amount, model);

            assertThat(view).isEqualTo("redirect:/dashboard");
            verify(accountService).findAccountByUsername(TEST_USERNAME);
            verify(accountService).transferAmount(fromAccount, toUsername, amount);
            verifyNoInteractions(model);
        }

        @Test
        @DisplayName("insufficient funds during transfer returns dashboard with error")
        void insufficientFundsDuringTransfer() {
            Account fromAccount = createTestAccount();
            String toUsername = "recipient";
            BigDecimal amount = new BigDecimal("5000.00");
            String errorMessage = "Insufficient funds";
            when(accountService.findAccountByUsername(TEST_USERNAME)).thenReturn(fromAccount);
            doThrow(new RuntimeException(errorMessage))
                    .when(accountService).transferAmount(fromAccount, toUsername, amount);

            String view = bankController.transferAmount(toUsername, amount, model);

            assertThat(view).isEqualTo("dashboard");
            verify(model).addAttribute("error", errorMessage);
            verify(model).addAttribute("account", fromAccount);
        }

        @Test
        @DisplayName("recipient not found returns dashboard with error")
        void recipientNotFound() {
            Account fromAccount = createTestAccount();
            String toUsername = "nonexistent";
            BigDecimal amount = new BigDecimal("100.00");
            String errorMessage = "Recipient account not found";
            when(accountService.findAccountByUsername(TEST_USERNAME)).thenReturn(fromAccount);
            doThrow(new RuntimeException(errorMessage))
                    .when(accountService).transferAmount(fromAccount, toUsername, amount);

            String view = bankController.transferAmount(toUsername, amount, model);

            assertThat(view).isEqualTo("dashboard");
            verify(model).addAttribute("error", errorMessage);
            verify(model).addAttribute("account", fromAccount);
        }

        @Test
        @DisplayName("generic runtime exception during transfer shows error on dashboard")
        void genericExceptionDuringTransfer() {
            Account fromAccount = createTestAccount();
            String toUsername = "recipient";
            BigDecimal amount = new BigDecimal("100.00");
            String errorMessage = "Transfer service error";
            when(accountService.findAccountByUsername(TEST_USERNAME)).thenReturn(fromAccount);
            doThrow(new RuntimeException(errorMessage))
                    .when(accountService).transferAmount(fromAccount, toUsername, amount);

            String view = bankController.transferAmount(toUsername, amount, model);

            assertThat(view).isEqualTo("dashboard");
            verify(model).addAttribute("error", errorMessage);
            verify(model).addAttribute("account", fromAccount);
        }
    }
}
