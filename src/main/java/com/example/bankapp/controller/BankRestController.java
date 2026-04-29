package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api")
public class BankRestController {

    @Autowired
    private AccountService accountService;

    @Autowired
    @Qualifier("paymentTaskExecutor")
    private Executor paymentTaskExecutor;

    @PostMapping("/deposit")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> deposit(@RequestParam BigDecimal amount) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return CompletableFuture.supplyAsync(() -> {
            Account account = accountService.findAccountByUsername(username);

            try {
                accountService.deposit(account, amount);
            } catch (RuntimeException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", e.getMessage());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Account updated = accountService.findAccountByUsername(username);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Deposit successful");
            response.put("newBalance", updated.getBalance());
            response.put("amount", amount);
            return ResponseEntity.ok(response);
        }, paymentTaskExecutor);
    }

    @PostMapping("/withdraw")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> withdraw(@RequestParam BigDecimal amount) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return CompletableFuture.supplyAsync(() -> {
            Account account = accountService.findAccountByUsername(username);

            try {
                accountService.withdraw(account, amount);
            } catch (RuntimeException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", e.getMessage());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Account updated = accountService.findAccountByUsername(username);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Withdrawal successful");
            response.put("newBalance", updated.getBalance());
            response.put("amount", amount);
            return ResponseEntity.ok(response);
        }, paymentTaskExecutor);
    }

    @PostMapping("/transfer")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> transfer(
            @RequestParam String toUsername, @RequestParam BigDecimal amount) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return CompletableFuture.supplyAsync(() -> {
            Account fromAccount = accountService.findAccountByUsername(username);

            try {
                accountService.transferAmount(fromAccount, toUsername, amount);
            } catch (RuntimeException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", e.getMessage());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Account updated = accountService.findAccountByUsername(username);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Transfer successful");
            response.put("newBalance", updated.getBalance());
            response.put("amount", amount);
            response.put("recipient", toUsername);
            return ResponseEntity.ok(response);
        }, paymentTaskExecutor);
    }

    @GetMapping("/transactions")
    public CompletableFuture<ResponseEntity<List<Transaction>>> transactions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return CompletableFuture.supplyAsync(() -> {
            Account account = accountService.findAccountByUsername(username);
            List<Transaction> history = accountService.getTransactionHistory(account);
            return ResponseEntity.ok(history);
        }, paymentTaskExecutor);
    }
}
