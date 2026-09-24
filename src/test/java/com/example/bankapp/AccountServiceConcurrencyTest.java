package com.example.bankapp;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AccountServiceConcurrencyTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    private Account newAccount(BigDecimal balance) {
        Account account = new Account();
        account.setUsername("conc-" + UUID.randomUUID());
        account.setPassword("x");
        account.setBalance(balance);
        return accountRepository.save(account);
    }

    @Test
    void concurrentWithdrawalsCannotOverdrawTheSameBalance() throws Exception {
        Account account = newAccount(new BigDecimal("100.00"));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Boolean> withdraw = () -> {
            barrier.await();
            try {
                accountService.withdraw(account, new BigDecimal("100.00"));
                return true;
            } catch (RuntimeException e) {
                return rejected(e);
            }
        };

        List<Future<Boolean>> results = pool.invokeAll(List.of(withdraw, withdraw));
        pool.shutdown();

        int succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                succeeded++;
            }
        }

        BigDecimal finalBalance = accountRepository.findById(account.getId()).orElseThrow().getBalance();
        assertEquals(1, succeeded, "exactly one of two concurrent withdrawals may succeed");
        assertEquals(0, finalBalance.compareTo(BigDecimal.ZERO), "balance must not be overdrawn");
    }

    @Test
    void concurrentTransfersOutOfTheSameAccountConserveTotalFunds() throws Exception {
        Account sender = newAccount(new BigDecimal("100.00"));
        Account first = newAccount(new BigDecimal("0.00"));
        Account second = newAccount(new BigDecimal("0.00"));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        List<Future<Boolean>> results = pool.invokeAll(List.of(
                transferTask(barrier, sender, first),
                transferTask(barrier, sender, second)));
        pool.shutdown();

        int succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                succeeded++;
            }
        }

        BigDecimal total = accountRepository.findById(sender.getId()).orElseThrow().getBalance()
                .add(accountRepository.findById(first.getId()).orElseThrow().getBalance())
                .add(accountRepository.findById(second.getId()).orElseThrow().getBalance());

        assertEquals(1, succeeded, "only one 100.00 transfer can clear a 100.00 balance");
        assertTrue(total.compareTo(new BigDecimal("100.00")) == 0, "no funds created or destroyed");
    }

    private Callable<Boolean> transferTask(CyclicBarrier barrier, Account sender, Account recipient) {
        return () -> {
            barrier.await();
            try {
                accountService.transferAmount(sender, recipient.getUsername(), new BigDecimal("100.00"));
                return true;
            } catch (RuntimeException e) {
                return rejected(e);
            }
        };
    }

    /** Only an insufficient-funds rejection is an expected outcome; anything else is a real failure. */
    private boolean rejected(RuntimeException e) {
        if (!"Insufficient funds".equals(e.getMessage())) {
            throw e;
        }
        return false;
    }

    @Test
    void concurrentDepositAndTransferIntoTheSameAccountKeepBothAmounts() throws Exception {
        Account sender = newAccount(new BigDecimal("10.00"));
        Account recipient = newAccount(new BigDecimal("20.00"));

        // as the controller does: loaded before the service transaction starts
        Account staleRecipient = accountRepository.findById(recipient.getId()).orElseThrow();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        List<Future<Boolean>> results = pool.invokeAll(List.of(
                (Callable<Boolean>) () -> {
                    barrier.await();
                    accountService.deposit(staleRecipient, new BigDecimal("5.00"));
                    return true;
                },
                (Callable<Boolean>) () -> {
                    barrier.await();
                    accountService.transferAmount(sender, recipient.getUsername(), new BigDecimal("10.00"));
                    return true;
                }));
        pool.shutdown();
        for (Future<Boolean> result : results) {
            result.get();
        }

        BigDecimal recipientBalance = accountRepository.findById(recipient.getId()).orElseThrow().getBalance();
        assertEquals(0, recipientBalance.compareTo(new BigDecimal("35.00")), "neither the deposit nor the transfer may be lost");
    }
}
