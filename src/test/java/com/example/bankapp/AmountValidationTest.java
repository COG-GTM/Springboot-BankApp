package com.example.bankapp;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "mallory")
class AmountValidationTest {

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
        createAccount("bob", "1000.00");
    }

    @Test
    void negativeTransferIsRejectedAndBalancesUnchanged() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "-500"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Amount must be greater than zero"));

        assertUnchanged();
    }

    @Test
    void zeroTransferIsRejected() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "0"))
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Amount must be greater than zero"));

        assertUnchanged();
    }

    @Test
    void negativeWithdrawalIsRejectedAndBalanceUnchanged() throws Exception {
        mockMvc.perform(post("/withdraw").param("amount", "-500"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Amount must be greater than zero"));

        assertUnchanged();
    }

    @Test
    void negativeDepositIsRejectedAndBalanceUnchanged() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "-500"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Amount must be greater than zero"));

        assertUnchanged();
    }

    @Test
    void transferToOwnAccountIsRejected() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "mallory").param("amount", "50"))
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", "Cannot transfer to your own account"));

        assertUnchanged();
    }

    @Test
    void positiveTransferStillMovesFunds() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "40.50"))
                .andExpect(redirectedUrl("/dashboard"));

        assertBalance("mallory", "59.50");
        assertBalance("bob", "1040.50");
        assertEquals(2, transactionRepository.count());
    }

    @Test
    void positiveDepositAndWithdrawalStillWork() throws Exception {
        mockMvc.perform(post("/deposit").param("amount", "25")).andExpect(redirectedUrl("/dashboard"));
        mockMvc.perform(post("/withdraw").param("amount", "10")).andExpect(redirectedUrl("/dashboard"));

        assertBalance("mallory", "115.00");
        assertEquals(2, transactionRepository.count());
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
        assertBalance("bob", "1000.00");
        assertEquals(0, transactionRepository.count());
    }

    private void assertBalance(String username, String expected) {
        BigDecimal actual = accountRepository.findByUsername(username).get().getBalance();
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                username + " balance expected " + expected + " but was " + actual);
    }
}
