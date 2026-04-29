package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BankApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    private void createUserWithBalance(String username, String password, BigDecimal balance) {
        accountService.registerAccount(username, password);
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            Account account = accountService.findAccountByUsername(username);
            accountService.deposit(account, balance);
        }
    }

    @Test
    void deposit_validAmount_returns200WithUpdatedBalance() throws Exception {
        createUserWithBalance("apiuser", "password123", BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/deposit")
                        .with(httpBasic("apiuser", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(500));
    }

    @Test
    void deposit_negativeAmount_returns400() throws Exception {
        createUserWithBalance("apiuser2", "password123", BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/deposit")
                        .with(httpBasic("apiuser2", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": -100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void withdraw_validAmount_returns200() throws Exception {
        createUserWithBalance("apiuser3", "password123", new BigDecimal("1000"));

        mockMvc.perform(post("/api/v1/withdraw")
                        .with(httpBasic("apiuser3", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 300}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(700));
    }

    @Test
    void withdraw_insufficientFunds_returns400() throws Exception {
        createUserWithBalance("apiuser4", "password123", new BigDecimal("100"));

        mockMvc.perform(post("/api/v1/withdraw")
                        .with(httpBasic("apiuser4", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 500}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void transfer_validRequest_returns200() throws Exception {
        createUserWithBalance("sender1", "password123", new BigDecimal("1000"));
        createUserWithBalance("receiver1", "password123", BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/transfer")
                        .with(httpBasic("sender1", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUsername\": \"receiver1\", \"amount\": 300}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(700));
    }

    @Test
    void transfer_recipientNotFound_returns404() throws Exception {
        createUserWithBalance("sender2", "password123", new BigDecimal("1000"));

        mockMvc.perform(post("/api/v1/transfer")
                        .with(httpBasic("sender2", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUsername\": \"nonexistent\", \"amount\": 100}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void transfer_insufficientFunds_returns400() throws Exception {
        createUserWithBalance("sender3", "password123", new BigDecimal("50"));
        createUserWithBalance("receiver3", "password123", BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/transfer")
                        .with(httpBasic("sender3", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUsername\": \"receiver3\", \"amount\": 500}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getBalance_authenticated_returns200() throws Exception {
        createUserWithBalance("balanceuser", "password123", new BigDecimal("750"));

        mockMvc.perform(get("/api/v1/balance")
                        .with(httpBasic("balanceuser", "password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(750))
                .andExpect(jsonPath("$.data.username").value("balanceuser"));
    }

    @Test
    void getTransactions_authenticated_returns200() throws Exception {
        createUserWithBalance("txnuser", "password123", new BigDecimal("500"));

        mockMvc.perform(get("/api/v1/transactions")
                        .with(httpBasic("txnuser", "password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void allEndpoints_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUsername\": \"x\", \"amount\": 100}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/balance"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized());
    }
}
