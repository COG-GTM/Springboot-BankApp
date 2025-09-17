package com.example.bankapp.client;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.BankingService;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(name = "micronaut-banking", url = "${micronaut.service.url:http://micronaut-app:8080}")
public interface MicronautBankingClient extends BankingService {

    @Override
    @GetMapping("/api/account")
    Account findAccountByUsername(@RequestParam("username") String username);

    @Override
    @PostMapping("/api/register")
    default Account registerAccount(@RequestParam("username") String username, @RequestParam("password") String password) {
        Map<String, String> response = registerAccountInternal(username, password);
        return findAccountByUsername(username);
    }

    @PostMapping("/api/register")
    Map<String, String> registerAccountInternal(@RequestParam("username") String username, @RequestParam("password") String password);

    @Override
    @PostMapping("/api/deposit")
    default void deposit(Account account, @RequestParam("amount") BigDecimal amount) {
        depositInternal(amount, account.getUsername());
    }

    @PostMapping("/api/deposit")
    Map<String, String> depositInternal(@RequestParam("amount") BigDecimal amount, @RequestParam("username") String username);

    @Override
    @PostMapping("/api/withdraw")
    default void withdraw(Account account, @RequestParam("amount") BigDecimal amount) {
        withdrawInternal(amount, account.getUsername());
    }

    @PostMapping("/api/withdraw")
    Map<String, String> withdrawInternal(@RequestParam("amount") BigDecimal amount, @RequestParam("username") String username);

    @Override
    @GetMapping("/api/transactions")
    default List<Transaction> getTransactionHistory(Account account) {
        return getTransactionHistoryInternal(account.getUsername());
    }

    @GetMapping("/api/transactions")
    List<Transaction> getTransactionHistoryInternal(@RequestParam("username") String username);

    @Override
    @PostMapping("/api/transfer")
    default void transferAmount(Account fromAccount, @RequestParam("toUsername") String toUsername, @RequestParam("amount") BigDecimal amount) {
        transferAmountInternal(toUsername, amount, fromAccount.getUsername());
    }

    @PostMapping("/api/transfer")
    Map<String, String> transferAmountInternal(@RequestParam("toUsername") String toUsername, @RequestParam("amount") BigDecimal amount, @RequestParam("fromUsername") String fromUsername);
}
