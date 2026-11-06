package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceConcurrencyTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void concurrentFullBalanceWithdrawalsLetExactlyOneSucceed() throws Exception {
        Account mallory = createAccount("mallory", "100.00");

        List<Boolean> results = runConcurrently(20, i -> {
            Account copy = accountService.findAccountByUsername("mallory");
            accountService.withdraw(copy, new BigDecimal("100.00"));
        });

        assertThat(results.stream().filter(ok -> ok).count()).isEqualTo(1);
        assertThat(balanceOf("mallory")).isEqualByComparingTo("0.00");
        assertThat(transactionRepository.findByAccountId(mallory.getId())).hasSize(1);
    }

    @Test
    void concurrentTransfersCannotSpendTheSameBalanceTwice() throws Exception {
        createAccount("mallory", "100.00");
        for (int i = 0; i < 10; i++) {
            createAccount("accomplice" + i, "0.00");
        }

        List<Boolean> results = runConcurrently(10, i -> {
            Account copy = accountService.findAccountByUsername("mallory");
            accountService.transferAmount(copy, "accomplice" + i, new BigDecimal("100.00"));
        });

        assertThat(results.stream().filter(ok -> ok).count()).isEqualTo(1);
        assertThat(balanceOf("mallory")).isEqualByComparingTo("0.00");
        BigDecimal received = BigDecimal.ZERO;
        for (int i = 0; i < 10; i++) {
            received = received.add(balanceOf("accomplice" + i));
        }
        assertThat(received).isEqualByComparingTo("100.00");
    }

    @Test
    void concurrentCreditsToOneRecipientAreNotLost() throws Exception {
        createAccount("alice", "0.00");
        createAccount("bob", "1000.00");
        createAccount("mallory", "100.00");

        List<Boolean> results = runConcurrently(21, i -> {
            if (i == 0) {
                Account bob = accountService.findAccountByUsername("bob");
                accountService.transferAmount(bob, "alice", new BigDecimal("1000.00"));
            } else {
                Account mallory = accountService.findAccountByUsername("mallory");
                accountService.transferAmount(mallory, "alice", new BigDecimal("0.01"));
            }
        });

        assertThat(results).containsOnly(true);
        assertThat(balanceOf("bob")).isEqualByComparingTo("0.00");
        assertThat(balanceOf("mallory")).isEqualByComparingTo("99.80");
        assertThat(balanceOf("alice")).isEqualByComparingTo("1000.20");
    }

    @Test
    void concurrentDepositsAreAllApplied() throws Exception {
        createAccount("alice", "0.00");

        List<Boolean> results = runConcurrently(20, i -> {
            Account copy = accountService.findAccountByUsername("alice");
            accountService.deposit(copy, new BigDecimal("5.00"));
        });

        assertThat(results).containsOnly(true);
        assertThat(balanceOf("alice")).isEqualByComparingTo("100.00");
    }

    @Test
    void oppositeDirectionTransfersDoNotDeadlockAndConserveMoney() throws Exception {
        createAccount("alice", "500.00");
        createAccount("bob", "500.00");

        List<Boolean> results = runConcurrently(20, i -> {
            String from = i % 2 == 0 ? "alice" : "bob";
            String to = i % 2 == 0 ? "bob" : "alice";
            accountService.transferAmount(accountService.findAccountByUsername(from), to, new BigDecimal("10.00"));
        });

        assertThat(results).containsOnly(true);
        assertThat(balanceOf("alice")).isEqualByComparingTo("500.00");
        assertThat(balanceOf("bob")).isEqualByComparingTo("500.00");
    }

    @Test
    void failedTransferRollsBackBothBalancesAndRecords() {
        Account sender = createAccount("sender", "100.00");
        // "Transfer Out to " + this name exceeds the 255-character transaction type column.
        String longName = "r".repeat(250);
        Account recipient = createAccount(longName, "0.00");

        assertThatThrownBy(() -> accountService.transferAmount(
                accountService.findAccountByUsername("sender"), longName, new BigDecimal("40.00")))
                .isInstanceOf(RuntimeException.class);

        assertThat(balanceOf("sender")).isEqualByComparingTo("100.00");
        assertThat(balanceOf(longName)).isEqualByComparingTo("0.00");
        assertThat(transactionRepository.findByAccountId(sender.getId())).isEmpty();
        assertThat(transactionRepository.findByAccountId(recipient.getId())).isEmpty();
    }

    private Account createAccount(String username, String balance) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("x");
        account.setBalance(new BigDecimal(balance));
        return accountRepository.save(account);
    }

    private BigDecimal balanceOf(String username) {
        return accountRepository.findByUsername(username).orElseThrow().getBalance();
    }

    private interface Task {
        void run(int index) throws Exception;
    }

    private List<Boolean> runConcurrently(int threads, Task task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int index = i;
                Callable<Boolean> call = () -> {
                    ready.countDown();
                    go.await();
                    try {
                        task.run(index);
                        return true;
                    } catch (RuntimeException e) {
                        return false;
                    }
                };
                futures.add(pool.submit(call));
            }
            ready.await(10, TimeUnit.SECONDS);
            go.countDown();
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> f : futures) {
                results.add(f.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
