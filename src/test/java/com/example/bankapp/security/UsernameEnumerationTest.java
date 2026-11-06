package com.example.bankapp.security;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsernameEnumerationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        saveAccount("alice", "alice-password", "100.00");
        saveAccount("bob", "bob-password", "0.00");
    }

    @Test
    void registerReturnsSameResponseForExistingAndNewUsernames() throws Exception {
        MockHttpServletResponse existing = mockMvc.perform(post("/register")
                        .param("username", "alice").param("password", "attacker-password"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"))
                .andReturn().getResponse();
        MockHttpServletResponse fresh = mockMvc.perform(post("/register")
                        .param("username", "carol").param("password", "carol-password"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"))
                .andReturn().getResponse();

        assertThat(existing.getContentAsString()).isEqualTo(fresh.getContentAsString());
        assertThat(accountRepository.findByUsername("carol")).isPresent();
        assertThat(accountRepository.findAll().stream().filter(a -> a.getUsername().equals("alice")).count())
                .isEqualTo(1);
        assertThat(passwordEncoder.matches("alice-password",
                accountRepository.findByUsername("alice").orElseThrow().getPassword())).isTrue();
    }

    @Test
    @WithMockUser(username = "alice")
    void zeroAmountTransferProbeGivesSameResultForExistingAndUnknownRecipient() throws Exception {
        for (String recipient : new String[]{"bob", "no-such-user"}) {
            mockMvc.perform(post("/transfer").param("toUsername", recipient).param("amount", "0"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard"))
                    .andExpect(model().attribute("error", AccountService.TRANSFER_FAILED_MESSAGE));
        }
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "alice")
    void transferToUnknownRecipientShowsGenericErrorAndKeepsBalance() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "no-such-user").param("amount", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("error", AccountService.TRANSFER_FAILED_MESSAGE));

        assertThat(accountRepository.findByUsername("alice").orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    @WithMockUser(username = "alice")
    void transferToExistingRecipientStillWorks() throws Exception {
        mockMvc.perform(post("/transfer").param("toUsername", "bob").param("amount", "10"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));

        assertThat(accountRepository.findByUsername("bob").orElseThrow().getBalance())
                .isEqualByComparingTo("10.00");
    }

    @Test
    void loginFailureLooksTheSameForUnknownUserAndWrongPassword() throws Exception {
        mockMvc.perform(formLogin("/login").user("alice").password("wrong-password"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error"));
        mockMvc.perform(formLogin("/login").user("no-such-user").password("wrong-password"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error"));
        mockMvc.perform(formLogin("/login").user("alice").password("alice-password"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));
    }

    private void saveAccount(String username, String password, String balance) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setBalance(new BigDecimal(balance));
        accountRepository.save(account);
    }
}
