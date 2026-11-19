package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AmountValidationIntegrationTest {

    private static final String HUGE = "1E+500000000";
    private static final Duration FAST = Duration.ofSeconds(5);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private String mallory;
    private String alice;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        mallory = "mallory-" + suffix;
        alice = "alice-" + suffix;
        accountService.registerAccount(mallory, "secret");
        accountService.registerAccount(alice, "secret");
    }

    @Test
    void depositWithHugeExponentIsRejectedQuicklyWith400() {
        assertTimeoutPreemptively(FAST, () -> mockMvc.perform(post("/deposit")
                        .param("amount", HUGE)
                        .with(user(mallory)))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Amount must not exceed 1000000000000")));

        assertUnchanged(mallory);
    }

    @Test
    void withdrawWithNegativeHugeExponentIsRejectedQuicklyWith400() {
        assertTimeoutPreemptively(FAST, () -> mockMvc.perform(post("/withdraw")
                        .param("amount", "-" + HUGE)
                        .with(user(mallory)))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute("error", "Amount must be greater than zero")));

        assertUnchanged(mallory);
    }

    @Test
    void transferWithNegativeHugeExponentIsRejectedQuicklyWith400() {
        assertTimeoutPreemptively(FAST, () -> mockMvc.perform(post("/transfer")
                        .param("toUsername", alice)
                        .param("amount", "-" + HUGE)
                        .with(user(mallory)))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute("error", "Amount must be greater than zero")));

        assertUnchanged(mallory);
        assertUnchanged(alice);
    }

    @Test
    void depositWithTinyNegativeExponentIsRejected() throws Exception {
        mockMvc.perform(post("/deposit")
                        .param("amount", "1E-500000000")
                        .with(user(mallory)))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute("error", "Amount must have at most 2 decimal places"));

        assertUnchanged(mallory);
    }

    @Test
    void validDepositWithdrawAndTransferStillWork() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "100.50").with(user(mallory)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
        mockMvc.perform(post("/withdraw").param("amount", "0.50").with(user(mallory)))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/transfer").param("toUsername", alice).param("amount", "40").with(user(mallory)))
                .andExpect(status().is3xxRedirection());

        assertThat(balanceOf(mallory)).isEqualByComparingTo("60.00");
        assertThat(balanceOf(alice)).isEqualByComparingTo("40.00");
    }

    private void assertUnchanged(String username) {
        Account account = accountRepository.findByUsername(username).orElseThrow();
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(transactionRepository.findByAccountId(account.getId())).isEmpty();
    }

    private BigDecimal balanceOf(String username) {
        return accountRepository.findByUsername(username).orElseThrow().getBalance();
    }
}
