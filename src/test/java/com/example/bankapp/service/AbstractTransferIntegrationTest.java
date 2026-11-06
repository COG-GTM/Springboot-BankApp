package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractTransferIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AccountService accountService;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        createAccount("mallory", "100.00");
        createAccount("bob", "0.00");
    }

    @Test
    void selfTransferOverHttpIsRejectedAndBalanceUnchanged() throws Exception {
        mockMvc.perform(post("/transfer").with(user("mallory"))
                        .param("toUsername", "mallory")
                        .param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Cannot transfer to your own account"));

        assertThat(balanceOf("mallory")).isEqualByComparingTo("100.00");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void selfTransferInServiceIsRejectedAndBalanceUnchanged() {
        Account mallory = accountService.findAccountByUsername("mallory");

        assertThatThrownBy(() -> accountService.transferAmount(mallory, "mallory", new BigDecimal("100")))
                .hasMessage("Cannot transfer to your own account");

        assertThat(balanceOf("mallory")).isEqualByComparingTo("100.00");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void transferToAnotherUserMovesMoney() throws Exception {
        mockMvc.perform(post("/transfer").with(user("mallory"))
                        .param("toUsername", "bob")
                        .param("amount", "40"))
                .andExpect(redirectedUrl("/dashboard"));

        assertThat(balanceOf("mallory")).isEqualByComparingTo("60.00");
        assertThat(balanceOf("bob")).isEqualByComparingTo("40.00");
        assertThat(transactionRepository.count()).isEqualTo(2);
    }

    @Test
    void transferUsesCurrentBalanceNotCallersCopy() {
        Account staleCopy = accountService.findAccountByUsername("mallory");
        staleCopy.setBalance(new BigDecimal("1000.00"));

        assertThatThrownBy(() -> accountService.transferAmount(staleCopy, "bob", new BigDecimal("500")))
                .hasMessage("Insufficient funds");

        assertThat(balanceOf("mallory")).isEqualByComparingTo("100.00");
        assertThat(balanceOf("bob")).isEqualByComparingTo("0.00");
    }

    private void createAccount(String username, String balance) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("unused");
        account.setBalance(new BigDecimal(balance));
        accountRepository.save(account);
    }

    private BigDecimal balanceOf(String username) {
        return accountRepository.findByUsername(username).orElseThrow().getBalance();
    }
}
