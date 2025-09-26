package com.example.bankapp.micronaut.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.micronaut.service.MicronautAccountService;
import com.example.bankapp.micronaut.dto.AccountRequest;
import com.example.bankapp.micronaut.dto.AccountResponse;
import com.example.bankapp.micronaut.dto.DepositWithdrawRequest;
import com.example.bankapp.micronaut.dto.TransactionRequest;
import com.example.bankapp.micronaut.dto.TransactionResponse;
import com.example.bankapp.micronaut.dto.TransferRequest;
import java.util.Map;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

import java.util.List;

@Controller("/api")
public class MicronautBankController {

    private final MicronautAccountService accountService;

    public MicronautBankController(MicronautAccountService accountService) {
        this.accountService = accountService;
    }

    @Post("/account/find")
    public HttpResponse<?> findAccount(@Body AccountRequest request) {
        try {
            Account account = accountService.findAccountByUsername(request.getUsername());
            AccountResponse response = new AccountResponse(account.getId(), account.getUsername(), account.getBalance());
            return HttpResponse.ok(response);
        } catch (Exception e) {
            return HttpResponse.serverError().body(Map.of("error", e.getMessage()));
        }
    }

    @Post("/test")
    public HttpResponse<?> test() {
        return HttpResponse.ok().body(Map.of("message", "Micronaut service is working"));
    }

    @Post("/account/register")
    public HttpResponse<?> registerAccount(@Body AccountRequest request) {
        try {
            Account account = accountService.registerAccount(request.getUsername(), request.getPassword());
            AccountResponse response = new AccountResponse(account.getId(), account.getUsername(), account.getBalance());
            return HttpResponse.ok(response);
        } catch (RuntimeException e) {
            return HttpResponse.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return HttpResponse.serverError().body(Map.of("error", e.getMessage()));
        }
    }

    @Post("/account/deposit")
    public HttpResponse<?> deposit(@Body DepositWithdrawRequest request) {
        try {
            Account account = accountService.findAccountByUsername(request.getUsername());
            accountService.deposit(account, request.getAmount());
            return HttpResponse.ok().body(Map.of("message", "Deposit successful"));
        } catch (Exception e) {
            return HttpResponse.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Post("/account/withdraw")
    public HttpResponse<?> withdraw(@Body DepositWithdrawRequest request) {
        try {
            Account account = accountService.findAccountByUsername(request.getUsername());
            accountService.withdraw(account, request.getAmount());
            return HttpResponse.ok().body(Map.of("message", "Withdrawal successful"));
        } catch (RuntimeException e) {
            return HttpResponse.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Post("/account/transactions")
    public HttpResponse<?> getTransactions(@Body TransactionRequest request) {
        try {
            Account account = accountService.findAccountByUsername(request.getUsername());
            List<Transaction> transactions = accountService.getTransactionHistory(account);
            List<TransactionResponse> transactionResponses = transactions.stream()
                .map(t -> new TransactionResponse(t.getId(), t.getAmount(), t.getDescription(), t.getTimestamp(), t.getAccount().getUsername()))
                .collect(java.util.stream.Collectors.toList());
            return HttpResponse.ok(transactionResponses);
        } catch (Exception e) {
            return HttpResponse.serverError().body(Map.of("error", e.getMessage()));
        }
    }

    @Post("/account/transfer")
    public HttpResponse<?> transfer(@Body TransferRequest request) {
        try {
            Account fromAccount = accountService.findAccountByUsername(request.getFromUsername());
            accountService.transferAmount(fromAccount, request.getToUsername(), request.getAmount());
            return HttpResponse.ok().body(Map.of("message", "Transfer successful"));
        } catch (RuntimeException e) {
            return HttpResponse.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
