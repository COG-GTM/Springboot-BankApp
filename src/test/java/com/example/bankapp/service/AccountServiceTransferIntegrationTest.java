package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceTransferIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @AfterEach
    void cleanUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void transferToOwnAccountDoesNotCreateMoney() {
        Account carol = accountService.registerAccount("carol", "secret");
        accountService.deposit(carol, new BigDecimal("100.00"));

        Account sender = accountService.findAccountByUsername("carol");
        assertThatThrownBy(() -> accountService.transferAmount(sender, "carol", new BigDecimal("40.00")))
                .isInstanceOf(RuntimeException.class);

        assertThat(accountService.findAccountByUsername("carol").getBalance()).isEqualByComparingTo("100.00");
    }
}
