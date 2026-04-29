package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class TransferConcurrencyTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long accountAId;
    private Long accountBId;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();

        Account accountA = new Account();
        accountA.setUsername("userA");
        accountA.setPassword(passwordEncoder.encode("password"));
        accountA.setBalance(new BigDecimal("1000"));
        accountA = accountRepository.save(accountA);
        accountAId = accountA.getId();

        Account accountB = new Account();
        accountB.setUsername("userB");
        accountB.setPassword(passwordEncoder.encode("password"));
        accountB.setBalance(new BigDecimal("1000"));
        accountB = accountRepository.save(accountB);
        accountBId = accountB.getId();
    }

    @Test
    void concurrentTransfers_maintainConsistency() throws InterruptedException {
        int threadCount = 10;
        BigDecimal transferAmount = new BigDecimal("10");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    Account from;
                    String toUsername;
                    if (index % 2 == 0) {
                        from = accountRepository.findById(accountAId).orElseThrow();
                        toUsername = "userB";
                    } else {
                        from = accountRepository.findById(accountBId).orElseThrow();
                        toUsername = "userA";
                    }
                    accountService.transferAmount(from, toUsername, transferAmount);
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Account finalA = accountRepository.findById(accountAId).orElseThrow();
        Account finalB = accountRepository.findById(accountBId).orElseThrow();
        BigDecimal totalMoney = finalA.getBalance().add(finalB.getBalance());

        assertEquals(new BigDecimal("2000").stripTrailingZeros(), totalMoney.stripTrailingZeros(),
                "Total money in the system should be conserved (2000). " +
                        "A=" + finalA.getBalance() + ", B=" + finalB.getBalance() +
                        ", failures=" + failures.get());
    }
}
