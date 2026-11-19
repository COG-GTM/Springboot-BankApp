package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Full-stack web-layer parity tests: real security filter chain, controller, service and H2 database.
 */
@SpringBootTest
@ActiveProfiles("test")
class BankControllerWebTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private Account givenAccount(String username, String password, String balance) {
        Account account = accountService.registerAccount(username, password);
        account.setBalance(new BigDecimal(balance));
        return accountRepository.save(account);
    }

    // Boot 3 / Security 6 redirects to "http://localhost/login"; Boot 4 / Security 7 to "/login".
    private static ResultMatcher redirectsToLogin() {
        return header().string("Location", endsWith("/login"));
    }

    private BigDecimal balanceOf(String username) {
        return accountRepository.findByUsername(username).orElseThrow().getBalance();
    }

    private List<Transaction> transactionsOf(String username) {
        return transactionRepository.findByAccountId(accountRepository.findByUsername(username).orElseThrow().getId());
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void registerPageIsPublic() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void registerCreatesAccountWithEncodedPasswordAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register").param("username", "alice").param("password", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        Account saved = accountRepository.findByUsername("alice").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("secret");
        assertThat(passwordEncoder.matches("secret", saved.getPassword())).isTrue();
        assertThat(saved.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void registerDuplicateUsernameRendersRegisterWithError() throws Exception {
        givenAccount("alice", "secret", "0");

        mockMvc.perform(post("/register").param("username", "alice").param("password", "other"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Username already exists"));

        assertThat(accountRepository.count()).isEqualTo(1);
    }

    @Test
    void protectedPagesRedirectAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectsToLogin());
        mockMvc.perform(get("/transactions")).andExpect(status().is3xxRedirection()).andExpect(redirectsToLogin());
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectsToLogin());
        mockMvc.perform(post("/deposit").param("amount", "10")).andExpect(status().is3xxRedirection()).andExpect(redirectsToLogin());
        mockMvc.perform(post("/withdraw").param("amount", "10")).andExpect(status().is3xxRedirection()).andExpect(redirectsToLogin());
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "10"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectsToLogin());
    }

    @Test
    void formLoginWithValidCredentialsRedirectsToDashboard() throws Exception {
        givenAccount("alice", "secret", "0");

        mockMvc.perform(formLogin("/login").user("alice").password("secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withUsername("alice"));
    }

    @Test
    void formLoginWithWrongPasswordRedirectsToLoginError() throws Exception {
        givenAccount("alice", "secret", "0");

        mockMvc.perform(formLogin("/login").user("alice").password("wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void formLoginWithUnknownUserRedirectsToLoginError() throws Exception {
        mockMvc.perform(formLogin("/login").user("ghost").password("secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void logoutRedirectsToLoginWithLogoutFlag() throws Exception {
        mockMvc.perform(post("/logout").with(user("alice")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
        mockMvc.perform(get("/logout").with(user("alice")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void dashboardShowsAuthenticatedUsersAccount() throws Exception {
        givenAccount("alice", "secret", "12.50");

        mockMvc.perform(get("/dashboard").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("account", hasProperty("username", is("alice"))))
                .andExpect(content().string(containsString("Welcome, alice")))
                .andExpect(content().string(containsString("Current Balance: $12.50")));
    }

    @Test
    void dashboardForAuthenticatedUserWithoutAccountFailsWithAccountNotFound() {
        assertThatThrownBy(() -> mockMvc.perform(get("/dashboard").with(user("ghost"))))
                .hasRootCauseMessage("Account not found");
    }

    @Test
    void depositIncreasesBalanceAndRecordsTransaction() throws Exception {
        givenAccount("alice", "secret", "10.00");

        mockMvc.perform(post("/deposit").with(user("alice")).param("amount", "15.25"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        assertThat(balanceOf("alice")).isEqualByComparingTo("25.25");
        assertThat(transactionsOf("alice")).extracting(Transaction::getType).containsExactly("Deposit");
    }

    @Test
    void depositWithNonNumericAmountIsBadRequest() throws Exception {
        givenAccount("alice", "secret", "10.00");

        mockMvc.perform(post("/deposit").with(user("alice")).param("amount", "abc"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/deposit").with(user("alice")))
                .andExpect(status().isBadRequest());

        assertThat(balanceOf("alice")).isEqualByComparingTo("10.00");
    }

    @Test
    void withdrawDecreasesBalanceAndRecordsTransaction() throws Exception {
        givenAccount("alice", "secret", "50.00");

        mockMvc.perform(post("/withdraw").with(user("alice")).param("amount", "20.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        assertThat(balanceOf("alice")).isEqualByComparingTo("30.00");
        assertThat(transactionsOf("alice")).extracting(Transaction::getType).containsExactly("Withdrawal");
    }

    @Test
    void withdrawWithInsufficientFundsRendersDashboardWithError() throws Exception {
        givenAccount("alice", "secret", "5.00");

        mockMvc.perform(post("/withdraw").with(user("alice")).param("amount", "20.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Insufficient funds"))
                .andExpect(model().attribute("account", hasProperty("username", is("alice"))))
                .andExpect(content().string(containsString("Insufficient funds")));

        assertThat(balanceOf("alice")).isEqualByComparingTo("5.00");
        assertThat(transactionsOf("alice")).isEmpty();
    }

    @Test
    void transferMovesFundsBetweenAccounts() throws Exception {
        givenAccount("alice", "secret", "100.00");
        givenAccount("bob", "secret", "1.00");

        mockMvc.perform(post("/transfer").with(user("alice")).param("toUsername", "bob").param("amount", "40.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        assertThat(balanceOf("alice")).isEqualByComparingTo("60.00");
        assertThat(balanceOf("bob")).isEqualByComparingTo("41.00");
        assertThat(transactionsOf("alice")).extracting(Transaction::getType).containsExactly("Transfer Out to bob");
        assertThat(transactionsOf("bob")).extracting(Transaction::getType).containsExactly("Transfer In from alice");
    }

    @Test
    void transferToUnknownRecipientRendersDashboardWithError() throws Exception {
        givenAccount("alice", "secret", "100.00");

        mockMvc.perform(post("/transfer").with(user("alice")).param("toUsername", "ghost").param("amount", "10.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Recipient account not found"));

        assertThat(balanceOf("alice")).isEqualByComparingTo("100.00");
    }

    @Test
    void transferWithInsufficientFundsRendersDashboardWithError() throws Exception {
        givenAccount("alice", "secret", "5.00");
        givenAccount("bob", "secret", "0.00");

        mockMvc.perform(post("/transfer").with(user("alice")).param("toUsername", "bob").param("amount", "10.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Insufficient funds"));

        assertThat(balanceOf("alice")).isEqualByComparingTo("5.00");
        assertThat(balanceOf("bob")).isEqualByComparingTo("0.00");
    }

    @Test
    void selfTransferLeavesBalanceUnchangedButRecordsTwoTransactions() throws Exception {
        givenAccount("alice", "secret", "10.00");

        mockMvc.perform(post("/transfer").with(user("alice")).param("toUsername", "alice").param("amount", "4.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        assertThat(balanceOf("alice")).isEqualByComparingTo("10.00");
        assertThat(transactionsOf("alice")).extracting(Transaction::getType)
                .containsExactly("Transfer Out to alice", "Transfer In from alice");
    }

    @Test
    void transactionsPageListsOnlyCurrentUsersHistory() throws Exception {
        givenAccount("alice", "secret", "0.00");
        givenAccount("bob", "secret", "0.00");
        mockMvc.perform(post("/deposit").with(user("alice")).param("amount", "30.00"));
        mockMvc.perform(post("/withdraw").with(user("alice")).param("amount", "10.00"));
        mockMvc.perform(post("/deposit").with(user("bob")).param("amount", "99.00"));

        mockMvc.perform(get("/transactions").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"))
                .andExpect(model().attribute("transactions", hasSize(2)))
                .andExpect(content().string(containsString("Deposit")))
                .andExpect(content().string(containsString("Withdrawal")));
    }
}
