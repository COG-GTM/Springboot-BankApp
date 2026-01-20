package com.example.bankapp.controller;

import com.example.bankapp.dto.*;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class BankApiController {

    private final AccountService accountService;

    public BankApiController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/account")
    public ResponseEntity<AccountDTO> getAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        AccountDTO accountDTO = new AccountDTO(account.getId(), account.getUsername(), account.getBalance());
        return ResponseEntity.ok(accountDTO);
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody DepositRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account account = accountService.findAccountByUsername(username);
            accountService.deposit(account, request.getAmount());
            
            Account updatedAccount = accountService.findAccountByUsername(username);
            AccountDTO accountDTO = new AccountDTO(updatedAccount.getId(), updatedAccount.getUsername(), updatedAccount.getBalance());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Deposit successful");
            response.put("account", accountDTO);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody WithdrawRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account account = accountService.findAccountByUsername(username);
            accountService.withdraw(account, request.getAmount());
            
            Account updatedAccount = accountService.findAccountByUsername(username);
            AccountDTO accountDTO = new AccountDTO(updatedAccount.getId(), updatedAccount.getUsername(), updatedAccount.getBalance());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Withdrawal successful");
            response.put("account", accountDTO);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody TransferRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account fromAccount = accountService.findAccountByUsername(username);
            accountService.transferAmount(fromAccount, request.getToUsername(), request.getAmount());
            
            Account updatedAccount = accountService.findAccountByUsername(username);
            AccountDTO accountDTO = new AccountDTO(updatedAccount.getId(), updatedAccount.getUsername(), updatedAccount.getBalance());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Transfer successful");
            response.put("account", accountDTO);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDTO>> getTransactions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        List<Transaction> transactions = accountService.getTransactionHistory(account);
        
        List<TransactionDTO> transactionDTOs = transactions.stream()
                .map(t -> new TransactionDTO(t.getId(), t.getAmount(), t.getType(), t.getTimestamp()))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(transactionDTOs);
    }
}
