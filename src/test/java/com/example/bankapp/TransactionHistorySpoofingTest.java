package com.example.bankapp;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.model.TransactionDirection;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionHistorySpoofingTest {

    private static final String SPOOF_NAME = "Transfer In refund desk";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    private Account account(String username, String balance) {
        Account a = new Account();
        a.setUsername(username);
        a.setPassword(passwordEncoder.encode("pw"));
        a.setBalance(new BigDecimal(balance));
        return accountRepository.save(a);
    }

    private static String amountCell(String html, String description) {
        Matcher m = Pattern.compile("<td>" + Pattern.quote(description) + "</td>\\s*<td class=\"([^\"]*)\">([^<]*)</td>")
                .matcher(html);
        assertThat(m.find()).as("row for %s in %s", description, html).isTrue();
        return m.group(1) + " " + m.group(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {SPOOF_NAME, "Transfer In x", "ab", "a234567890123456789012345678901", "bob<b>", "call 555-0100 now", ""})
    void registerRejectsUnsafeUsernames(String username) {
        assertThatThrownBy(() -> accountService.registerAccount(username, "pw"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(accountRepository.findAll()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"bob", "alice.b-1_", "a23456789012345678901234567890"})
    void registerAcceptsSafeUsernames(String username) {
        accountService.registerAccount(username, "pw");
        assertThat(accountRepository.findByUsername(username)).isPresent();
    }

    @Test
    void registerFormShowsValidationErrorAndCreatesNothing() throws Exception {
        String html = mockMvc.perform(post("/register").with(csrf())
                        .param("username", SPOOF_NAME).param("password", "pw"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Username must be 3-30 characters");
        assertThat(accountRepository.findByUsername(SPOOF_NAME)).isEmpty();
    }

    @Test
    void transferStoresDirectionAndCounterpartySeparately() {
        Account alice = account("alice", "500");
        account("bob", "0");

        accountService.transferAmount(alice, "bob", new BigDecimal("200"));

        Transaction out = transactionRepository.findByAccountId(alice.getId()).get(0);
        assertThat(out.getType()).isEqualTo("Transfer Out");
        assertThat(out.getDirection()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(out.getCounterparty()).isEqualTo("bob");
        assertThat(out.getDescription()).isEqualTo("Transfer Out to bob");

        Transaction in = transactionRepository.findByAccountId(accountRepository.findByUsername("bob").get().getId()).get(0);
        assertThat(in.getType()).isEqualTo("Transfer In");
        assertThat(in.getDirection()).isEqualTo(TransactionDirection.CREDIT);
        assertThat(in.getCounterparty()).isEqualTo("alice");
        assertThat(in.getDescription()).isEqualTo("Transfer In from alice");
    }

    @Test
    void outgoingTransferToSpoofingUsernameRendersAsDebit() throws Exception {
        // Account created before username rules existed.
        Account alice = account("alice", "500");
        account(SPOOF_NAME, "0");

        accountService.transferAmount(alice, SPOOF_NAME, new BigDecimal("200"));

        String html = mockMvc.perform(get("/transactions").with(user("alice")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(amountCell(html, "Transfer Out to " + SPOOF_NAME)).isEqualTo("text-danger 200.00");
    }

    @Test
    void incomingTransferFromSpoofingUsernameStillRendersAsCredit() throws Exception {
        Account mallory = account("Transfer Out to x", "1");
        account("alice", "0");

        accountService.transferAmount(mallory, "alice", new BigDecimal("0.01"));

        String html = mockMvc.perform(get("/transactions").with(user("alice")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(amountCell(html, "Transfer In from Transfer Out to x")).isEqualTo("text-success 0.01");
    }

    @Test
    void legacyRowsWithoutDirectionUseFixedPrefixOnly() throws Exception {
        Account alice = account("alice", "0");
        transactionRepository.saveAll(List.of(
                new Transaction(new BigDecimal("1000.00"), "Deposit", LocalDateTime.now(), alice),
                new Transaction(new BigDecimal("300.00"), "Withdrawal", LocalDateTime.now(), alice),
                new Transaction(new BigDecimal("200.00"), "Transfer Out to " + SPOOF_NAME, LocalDateTime.now(), alice),
                new Transaction(new BigDecimal("5.00"), "Transfer In from bob", LocalDateTime.now(), alice)));

        String html = mockMvc.perform(get("/transactions").with(user("alice")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(amountCell(html, "Deposit")).isEqualTo("text-success 1000.00");
        assertThat(amountCell(html, "Withdrawal")).isEqualTo("text-danger 300.00");
        assertThat(amountCell(html, "Transfer Out to " + SPOOF_NAME)).isEqualTo("text-danger 200.00");
        assertThat(amountCell(html, "Transfer In from bob")).isEqualTo("text-success 5.00");
    }
}
