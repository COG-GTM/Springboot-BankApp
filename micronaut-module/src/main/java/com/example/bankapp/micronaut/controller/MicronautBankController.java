package com.example.bankapp.micronaut.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.micronaut.service.MicronautAccountService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MicronautBankController {

    private final MicronautAccountService accountService;

    public MicronautBankController(MicronautAccountService accountService) {
        this.accountService = accountService;
    }

    @Get("/api/account")
    public HttpResponse<Account> getAccount(@QueryValue String username) {
        try {
            Account account = accountService.findAccountByUsername(username);
            return HttpResponse.ok(account);
        } catch (Exception e) {
            return HttpResponse.serverError();
        }
    }

    @Post("/api/register")
    public HttpResponse<Map<String, String>> registerAccount(@QueryValue String username, 
                                                           @QueryValue String password) {
        try {
            accountService.registerAccount(username, password);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Account registered successfully");
            return HttpResponse.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return HttpResponse.badRequest(error);
        }
    }

    @Post("/api/deposit")
    public HttpResponse<Map<String, String>> deposit(@QueryValue BigDecimal amount, @QueryValue String username) {
        try {
            Account account = accountService.findAccountByUsername(username);
            accountService.deposit(account, amount);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Deposit successful");
            return HttpResponse.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return HttpResponse.badRequest(error);
        }
    }

    @Post("/api/withdraw")
    public HttpResponse<Map<String, String>> withdraw(@QueryValue BigDecimal amount, @QueryValue String username) {
        try {
            Account account = accountService.findAccountByUsername(username);
            accountService.withdraw(account, amount);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Withdrawal successful");
            return HttpResponse.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return HttpResponse.badRequest(error);
        }
    }

    @Get("/api/transactions")
    public HttpResponse<?> getTransactions(@QueryValue String username) {
        try {
            Account account = accountService.findAccountByUsername(username);
            return HttpResponse.ok(accountService.getTransactionHistory(account));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return HttpResponse.serverError(error);
        }
    }

    @Post("/api/transfer")
    public HttpResponse<Map<String, String>> transfer(@QueryValue String toUsername, 
                                                    @QueryValue BigDecimal amount, 
                                                    @QueryValue String fromUsername) {
        try {
            Account fromAccount = accountService.findAccountByUsername(fromUsername);
            accountService.transferAmount(fromAccount, toUsername, amount);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Transfer successful");
            return HttpResponse.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return HttpResponse.badRequest(error);
        }
    }
}
