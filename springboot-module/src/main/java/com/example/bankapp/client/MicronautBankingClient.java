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
    Account findAccountByUsername(@RequestParam(value = "username") String username);

    @PostMapping("/api/register")
    Map<String, String> registerAccountRaw(@RequestParam(value = "username") String username, 
                                          @RequestParam(value = "password") String password);

    @PostMapping("/api/deposit")
    Map<String, String> depositRaw(@RequestParam(value = "amount") BigDecimal amount, 
                                  @RequestParam(value = "username") String username);

    @PostMapping("/api/withdraw")
    Map<String, String> withdrawRaw(@RequestParam(value = "amount") BigDecimal amount, 
                                   @RequestParam(value = "username") String username);

    @GetMapping("/api/transactions")
    List<Transaction> getTransactionsByUsername(@RequestParam(value = "username") String username);

    @PostMapping("/api/transfer")
    Map<String, String> transferRaw(@RequestParam(value = "toUsername") String toUsername,
                                   @RequestParam(value = "amount") BigDecimal amount,
                                   @RequestParam(value = "fromUsername") String fromUsername);
}
