package com.example.bankapp;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "mallory")
class NegativeAmountTransferTest {

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
        createAccount("mallory", "100.00");
        createAccount("alice", "1000.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-5000", "-1", "0", "0.001", "1.005", "1000000.01", "1E+20"})
    void transferWithInvalidAmountIsRejected(String amount) throws Exception {
        mockMvc.perform(post("/transfer").with(csrf()).param("toUsername", "alice").param("amount", amount))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));

        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-5000", "-1", "0", "0.001", "1000000.01"})
    void withdrawWithInvalidAmountIsRejected(String amount) throws Exception {
        mockMvc.perform(post("/withdraw").with(csrf()).param("amount", amount))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));

        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-5000", "-1", "0", "0.001", "1000000.01"})
    void depositWithInvalidAmountIsRejected(String amount) throws Exception {
        mockMvc.perform(post("/deposit").with(csrf()).param("amount", amount))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));

        assertUnchanged();
    }

    @Test
    void transferToOwnAccountIsRejected() throws Exception {
        mockMvc.perform(post("/transfer").with(csrf()).param("toUsername", "mallory").param("amount", "50"))
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Cannot transfer to your own account"));

        assertUnchanged();
    }

    @Test
    void validTransferStillMovesFunds() throws Exception {
        mockMvc.perform(post("/transfer").with(csrf()).param("toUsername", "alice").param("amount", "40.50"))
                .andExpect(redirectedUrl("/dashboard"));

        assertBalance("mallory", "59.50");
        assertBalance("alice", "1040.50");
        assertEquals(2, transactionRepository.count());
    }

    @Test
    void validDepositAndWithdrawalStillWork() throws Exception {
        mockMvc.perform(post("/deposit").with(csrf()).param("amount", "25.00")).andExpect(redirectedUrl("/dashboard"));
        mockMvc.perform(post("/withdraw").with(csrf()).param("amount", "10")).andExpect(redirectedUrl("/dashboard"));

        assertBalance("mallory", "115.00");
        assertEquals(2, transactionRepository.count());
    }

    @Test
    void databaseRejectsNegativeBalance() {
        Account account = new Account();
        account.setUsername("overdrawn");
        account.setPassword("unused");
        account.setBalance(new BigDecimal("-0.01"));

        assertThrows(DataIntegrityViolationException.class, () -> accountRepository.saveAndFlush(account));
    }

    private void createAccount(String username, String balance) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("unused");
        account.setBalance(new BigDecimal(balance));
        accountRepository.save(account);
    }

    private void assertUnchanged() {
        assertBalance("mallory", "100.00");
        assertBalance("alice", "1000.00");
        assertEquals(0, transactionRepository.count());
    }

    private void assertBalance(String username, String expected) {
        BigDecimal actual = accountRepository.findByUsername(username).orElseThrow().getBalance();
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                username + " balance expected " + expected + " but was " + actual);
    }
}
