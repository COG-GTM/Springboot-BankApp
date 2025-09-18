package com.example.bankapp.client;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(name = "micronaut-service", url = "${micronaut.service.url}")
public interface MicronautBankingClient {

    @GetMapping("/api/account")
    Account findAccountByUsername(@RequestParam("username") String username);

    @PostMapping("/api/register")
    Map<String, String> registerAccountRaw(@RequestParam("username") String username, 
                                          @RequestParam("password") String password);

    @PostMapping("/api/deposit")
    Map<String, String> depositRaw(@RequestParam("amount") BigDecimal amount, 
                                  @RequestParam("username") String username);

    @PostMapping("/api/withdraw")
    Map<String, String> withdrawRaw(@RequestParam("amount") BigDecimal amount, 
                                   @RequestParam("username") String username);

    @GetMapping("/api/transactions")
    List<Transaction> getTransactionsByUsername(@RequestParam("username") String username);

    @PostMapping("/api/transfer")
    Map<String, String> transferRaw(@RequestParam("toUsername") String toUsername,
                                   @RequestParam("amount") BigDecimal amount,
                                   @RequestParam("fromUsername") String fromUsername);
}
