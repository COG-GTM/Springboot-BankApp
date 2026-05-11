package com.example.bankapp.controller;

import com.example.bankapp.dto.TransactionResponse;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class TransactionApiController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/transactions/recent")
    public List<TransactionResponse> getRecentTransactions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        List<Transaction> transactions = accountService.getRecentTransactions(account);
        return transactions.stream()
                .map(t -> new TransactionResponse(t.getId(), t.getAmount(), t.getType(), t.getTimestamp()))
                .collect(Collectors.toList());
    }
}
