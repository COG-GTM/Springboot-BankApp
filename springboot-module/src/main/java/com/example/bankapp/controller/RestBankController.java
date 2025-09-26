package com.example.bankapp.controller;

import com.example.bankapp.facade.BankingServiceFacade;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RestBankController {

    @Autowired
    private BankingServiceFacade bankingServiceFacade;

    @PostMapping("/account/register")
    public ResponseEntity<?> registerAccount(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            String password = (String) request.get("password");
            Account account = bankingServiceFacade.registerAccount(username, password);
            return ResponseEntity.ok(account);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/account/find")
    public ResponseEntity<?> findAccount(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            Account account = bankingServiceFacade.findAccountByUsername(username);
            return ResponseEntity.ok(account);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/account/deposit")
    public ResponseEntity<?> deposit(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            Account account = bankingServiceFacade.findAccountByUsername(username);
            Account updatedAccount = bankingServiceFacade.deposit(account, amount);
            return ResponseEntity.ok(updatedAccount);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/account/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            Account account = bankingServiceFacade.findAccountByUsername(username);
            Account updatedAccount = bankingServiceFacade.withdraw(account, amount);
            return ResponseEntity.ok(updatedAccount);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/account/transfer")
    public ResponseEntity<?> transfer(@RequestBody Map<String, Object> request) {
        try {
            String fromUsername = (String) request.get("fromUsername");
            String toUsername = (String) request.get("toUsername");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            Account fromAccount = bankingServiceFacade.findAccountByUsername(fromUsername);
            bankingServiceFacade.transferAmount(fromAccount, toUsername, amount);
            return ResponseEntity.ok(Map.of("message", "Transfer successful"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/account/transactions")
    public ResponseEntity<?> getTransactions(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            Account account = bankingServiceFacade.findAccountByUsername(username);
            List<Transaction> transactions = bankingServiceFacade.getTransactionHistory(account);
            return ResponseEntity.ok(transactions);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
