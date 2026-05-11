package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        testAccount = new Account();
        testAccount.setUsername("testuser");
        testAccount.setPassword(passwordEncoder.encode("password"));
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount = accountRepository.save(testAccount);

        // Recent transaction (within 30 days)
        Transaction recent = new Transaction(
                new BigDecimal("50.00"), "Deposit", LocalDateTime.now().minusDays(5), testAccount);
        transactionRepository.save(recent);

        // Old transaction (older than 30 days)
        Transaction old = new Transaction(
                new BigDecimal("200.00"), "Withdrawal", LocalDateTime.now().minusDays(45), testAccount);
        transactionRepository.save(old);
    }

    @Test
    void recentTransactions_authenticated_returnsOnlyRecentEntries() throws Exception {
        mockMvc.perform(get("/api/transactions/recent").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amount", is(50.00)))
                .andExpect(jsonPath("$[0].type", is("Deposit")));
    }

    @Test
    void recentTransactions_unauthenticated_returnsUnauthorizedOrRedirect() throws Exception {
        mockMvc.perform(get("/api/transactions/recent"))
                .andExpect(status().is3xxRedirection());
    }
}
