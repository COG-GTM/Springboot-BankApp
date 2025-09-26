package com.example.bankapp.micronaut.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.micronaut.service.MicronautAccountService;
import com.example.bankapp.micronaut.dto.AccountRequest;
import com.example.bankapp.micronaut.dto.DepositWithdrawRequest;
import com.example.bankapp.micronaut.dto.TransactionRequest;
import com.example.bankapp.micronaut.dto.TransferRequest;
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
    public HttpResponse<Account> findAccount(@Body AccountRequest request) {
        try {
            Account account = accountService.findAccountByUsername(request.getUsername());
            return HttpResponse.ok(account);
        } catch (Exception e) {
            return HttpResponse.serverError();
        }
    }

    @Post("/account/register")
    public HttpResponse<Account> registerAccount(@Body AccountRequest request) {
        try {
            Account account = accountService.registerAccount(request.getUsername(), request.getPassword());
            return HttpResponse.ok(account);
        } catch (RuntimeException e) {
            return HttpResponse.badRequest();
        }
    }

    @Post("/account/deposit")
    public HttpResponse<Void> deposit(@Body DepositWithdrawRequest request) {
        try {
            accountService.deposit(request.getAccount(), request.getAmount());
            return HttpResponse.ok();
        } catch (Exception e) {
            return HttpResponse.badRequest();
        }
    }

    @Post("/account/withdraw")
    public HttpResponse<Void> withdraw(@Body DepositWithdrawRequest request) {
        try {
            accountService.withdraw(request.getAccount(), request.getAmount());
            return HttpResponse.ok();
        } catch (RuntimeException e) {
            return HttpResponse.badRequest();
        }
    }

    @Post("/account/transactions")
    public HttpResponse<List<Transaction>> getTransactions(@Body TransactionRequest request) {
        try {
            List<Transaction> transactions = accountService.getTransactionHistory(request.getAccount());
            return HttpResponse.ok(transactions);
        } catch (Exception e) {
            return HttpResponse.serverError();
        }
    }

    @Post("/account/transfer")
    public HttpResponse<Void> transfer(@Body TransferRequest request) {
        try {
            accountService.transferAmount(request.getFromAccount(), request.getToUsername(), request.getAmount());
            return HttpResponse.ok();
        } catch (RuntimeException e) {
            return HttpResponse.badRequest();
        }
    }
}
