package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AccountServiceConcurrencyTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        Account alice = accountService.registerAccount("alice", "password");
        alice.setBalance(new BigDecimal("100"));
        accountRepository.save(alice);

        Account bob = accountService.registerAccount("bob", "password");
        bob.setBalance(BigDecimal.ZERO);
        accountRepository.save(bob);
    }

    @Test
    void concurrentTransfersMustNotOverdraw() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(threadCount);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    Account from = accountService.findAccountByUsername("alice");
                    accountService.transferAmount(from, "bob", new BigDecimal("100"));
                    successes.incrementAndGet();
                } catch (RuntimeException e) {
                    failures.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failures.incrementAndGet();
                } finally {
                    finished.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(finished.await(30, TimeUnit.SECONDS))
                .as("all concurrent transfer tasks should finish within 30 seconds")
                .isTrue();
        executor.shutdown();

        Account alice = accountRepository.findByUsername("alice").orElseThrow();
        Account bob = accountRepository.findByUsername("bob").orElseThrow();

        assertThat(alice.getBalance().compareTo(BigDecimal.ZERO))
                .as("alice must not have a negative balance after concurrent transfers")
                .isGreaterThanOrEqualTo(0);
        assertThat(alice.getBalance().add(bob.getBalance()))
                .as("alice and bob balances must conserve the initial total")
                .isEqualByComparingTo("100");
        assertThat(successes.get())
                .as("exactly one transfer should succeed from alice's 100 balance")
                .isEqualTo(1);
        assertThat(failures.get())
                .as("all other transfers should fail after alice's funds are exhausted")
                .isEqualTo(threadCount - 1);
    }

    @Test
    void concurrentTransfersInOppositeDirectionsDoNotDeadlock() throws InterruptedException {
        Account bob = accountRepository.findByUsername("bob").orElseThrow();
        bob.setBalance(new BigDecimal("100"));
        accountRepository.save(bob);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Throwable> unexpectedErrors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            boolean aliceToBob = i < threadCount / 2;
            executor.submit(() -> {
                try {
                    startGate.await();
                    String fromUsername = aliceToBob ? "alice" : "bob";
                    Account from = accountService.findAccountByUsername(fromUsername);
                    accountService.transferAmount(from, aliceToBob ? "bob" : "alice", new BigDecimal("10"));
                } catch (RuntimeException e) {
                    if (!"Insufficient funds".equals(e.getMessage())) {
                        unexpectedErrors.add(e);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    unexpectedErrors.add(e);
                } finally {
                    finished.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(finished.await(30, TimeUnit.SECONDS))
                .as("all opposite-direction transfer tasks should finish without deadlocking")
                .isTrue();
        executor.shutdown();

        Account alice = accountRepository.findByUsername("alice").orElseThrow();
        bob = accountRepository.findByUsername("bob").orElseThrow();

        assertThat(unexpectedErrors)
                .as("opposite-direction transfers must not produce unexpected exceptions")
                .isEmpty();
        assertThat(alice.getBalance().add(bob.getBalance()))
                .as("alice and bob balances must conserve the initial total")
                .isEqualByComparingTo("200");
    }
}
