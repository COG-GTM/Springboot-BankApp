package com.example.bankapp.controller;

import com.example.bankapp.dto.*;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class BankApiController {

    private final AccountService accountService;

    public BankApiController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<AccountResponse>> deposit(@RequestBody DepositRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        accountService.deposit(account, request.getAmount());
        Account updated = accountService.findAccountByUsername(username);
        return ResponseEntity.ok(ApiResponse.ok("Deposit successful", AccountResponse.from(updated)));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<AccountResponse>> withdraw(@RequestBody WithdrawRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        accountService.withdraw(account, request.getAmount());
        Account updated = accountService.findAccountByUsername(username);
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal successful", AccountResponse.from(updated)));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<AccountResponse>> transfer(@RequestBody TransferRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account fromAccount = accountService.findAccountByUsername(username);
        accountService.transferAmount(fromAccount, request.getToUsername(), request.getAmount());
        Account updated = accountService.findAccountByUsername(username);
        return ResponseEntity.ok(ApiResponse.ok("Transfer successful", AccountResponse.from(updated)));
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<AccountResponse>> getBalance() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        return ResponseEntity.ok(ApiResponse.ok("Balance retrieved", AccountResponse.from(account)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        List<Transaction> transactions = accountService.getTransactionHistory(account);
        List<TransactionResponse> response = transactions.stream()
                .map(TransactionResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Transactions retrieved", response));
    }
}
