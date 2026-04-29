package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TransferTransactionalTest {

    @Autowired
    private AccountService accountService;

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
    }

    @Test
    @Transactional
    void transfer_shouldBeAtomic_bothAccountsUpdated() {
        Account sender = new Account();
        sender.setUsername("sender");
        sender.setPassword(passwordEncoder.encode("password"));
        sender.setBalance(new BigDecimal("1000.00"));
        sender = accountRepository.save(sender);

        Account recipient = new Account();
        recipient.setUsername("recipient");
        recipient.setPassword(passwordEncoder.encode("password"));
        recipient.setBalance(new BigDecimal("500.00"));
        recipient = accountRepository.save(recipient);

        accountService.transferAmount(sender, "recipient", new BigDecimal("300.00"));

        Account updatedSender = accountRepository.findByUsername("sender").orElseThrow();
        Account updatedRecipient = accountRepository.findByUsername("recipient").orElseThrow();

        assertEquals(0, updatedSender.getBalance().compareTo(new BigDecimal("700.00")));
        assertEquals(0, updatedRecipient.getBalance().compareTo(new BigDecimal("800.00")));
    }

    @Test
    void transfer_withInsufficientFunds_shouldNotChangeAnyBalance() {
        Account sender = new Account();
        sender.setUsername("sender2");
        sender.setPassword(passwordEncoder.encode("password"));
        sender.setBalance(new BigDecimal("100.00"));
        sender = accountRepository.save(sender);

        Account recipient = new Account();
        recipient.setUsername("recipient2");
        recipient.setPassword(passwordEncoder.encode("password"));
        recipient.setBalance(new BigDecimal("500.00"));
        recipient = accountRepository.save(recipient);

        final Account finalSender = sender;
        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(finalSender, "recipient2", new BigDecimal("200.00")));

        Account unchangedSender = accountRepository.findByUsername("sender2").orElseThrow();
        Account unchangedRecipient = accountRepository.findByUsername("recipient2").orElseThrow();

        assertEquals(0, unchangedSender.getBalance().compareTo(new BigDecimal("100.00")));
        assertEquals(0, unchangedRecipient.getBalance().compareTo(new BigDecimal("500.00")));
    }

    @Test
    void transfer_toNonExistentRecipient_shouldNotDebitSender() {
        Account sender = new Account();
        sender.setUsername("sender3");
        sender.setPassword(passwordEncoder.encode("password"));
        sender.setBalance(new BigDecimal("1000.00"));
        sender = accountRepository.save(sender);

        final Account finalSender = sender;
        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(finalSender, "ghost", new BigDecimal("100.00")));

        Account unchangedSender = accountRepository.findByUsername("sender3").orElseThrow();
        assertEquals(0, unchangedSender.getBalance().compareTo(new BigDecimal("1000.00")));
    }

    @Test
    @Transactional
    void deposit_followedByWithdraw_shouldReflectCorrectBalance() {
        Account account = new Account();
        account.setUsername("balanceuser");
        account.setPassword(passwordEncoder.encode("password"));
        account.setBalance(BigDecimal.ZERO);
        account = accountRepository.save(account);

        accountService.deposit(account, new BigDecimal("500.00"));
        accountService.withdraw(account, new BigDecimal("200.00"));

        Account updated = accountRepository.findByUsername("balanceuser").orElseThrow();
        assertEquals(0, updated.getBalance().compareTo(new BigDecimal("300.00")));
    }

    @Test
    void multipleTransfers_shouldMaintainConsistentTotals() {
        Account sender = new Account();
        sender.setUsername("multisender");
        sender.setPassword(passwordEncoder.encode("password"));
        sender.setBalance(new BigDecimal("1000.00"));
        sender = accountRepository.save(sender);

        Account recipient = new Account();
        recipient.setUsername("multirecipient");
        recipient.setPassword(passwordEncoder.encode("password"));
        recipient.setBalance(new BigDecimal("0.00"));
        recipient = accountRepository.save(recipient);

        for (int i = 0; i < 5; i++) {
            sender = accountRepository.findByUsername("multisender").orElseThrow();
            accountService.transferAmount(sender, "multirecipient", new BigDecimal("100.00"));
        }

        Account finalSender = accountRepository.findByUsername("multisender").orElseThrow();
        Account finalRecipient = accountRepository.findByUsername("multirecipient").orElseThrow();

        assertEquals(0, finalSender.getBalance().compareTo(new BigDecimal("500.00")));
        assertEquals(0, finalRecipient.getBalance().compareTo(new BigDecimal("500.00")));

        BigDecimal total = finalSender.getBalance().add(finalRecipient.getBalance());
        assertEquals(0, total.compareTo(new BigDecimal("1000.00")),
                "Total money in the system should remain constant");
    }
}
