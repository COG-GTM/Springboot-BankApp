package com.example.bankapp.security;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "mallory")
class TransactionAmountValidationTest {

    private static final BigDecimal MALLORY_BALANCE = new BigDecimal("100.00");
    private static final BigDecimal ALICE_BALANCE = new BigDecimal("5000.00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        accountRepository.save(account("mallory", MALLORY_BALANCE));
        accountRepository.save(account("alice", ALICE_BALANCE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-5000", "-1", "0", "0.001", "1000000000"})
    void transferRejectsInvalidAmount(String amount) throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "alice").param("amount", amount))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));

        assertBalancesUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-5000", "-1", "0", "0.001", "1000000000"})
    void withdrawRejectsInvalidAmount(String amount) throws Exception {
        mockMvc.perform(post("/withdraw").param("amount", amount))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));

        assertBalancesUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-5000", "-1", "0", "0.001", "1000000000"})
    void depositRejectsInvalidAmount(String amount) throws Exception {
        mockMvc.perform(post("/deposit").param("amount", amount))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));

        assertBalancesUnchanged();
    }

    @org.junit.jupiter.api.Test
    void validTransferStillMovesFunds() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "alice").param("amount", "25.50"))
                .andExpect(status().is3xxRedirection());

        assertThat(balanceOf("mallory")).isEqualByComparingTo("74.50");
        assertThat(balanceOf("alice")).isEqualByComparingTo("5025.50");
    }

    private void assertBalancesUnchanged() {
        assertThat(balanceOf("mallory")).isEqualByComparingTo(MALLORY_BALANCE);
        assertThat(balanceOf("alice")).isEqualByComparingTo(ALICE_BALANCE);
        assertThat(transactionRepository.count()).isZero();
    }

    private BigDecimal balanceOf(String username) {
        return accountRepository.findByUsername(username).orElseThrow().getBalance();
    }

    private static Account account(String username, BigDecimal balance) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("{noop}unused");
        account.setBalance(balance);
        return account;
    }
}
