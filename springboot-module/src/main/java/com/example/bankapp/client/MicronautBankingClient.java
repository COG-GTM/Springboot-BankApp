package com.example.bankapp.client;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(name = "micronaut-service", url = "${micronaut.service.url}", configuration = com.example.bankapp.config.FeignConfig.class)
public interface MicronautBankingClient {

    @GetMapping("/api/account")
    Account findAccountByUsername(@RequestParam(value = "username") String username);

    @PostMapping(value = "/api/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, String> registerAccountRaw(@RequestBody Map<String, String> request);

    @PostMapping(value = "/api/deposit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, String> depositRaw(@RequestBody Map<String, Object> request);

    @PostMapping(value = "/api/withdraw", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, String> withdrawRaw(@RequestBody Map<String, Object> request);

    @GetMapping("/api/transactions")
    List<Transaction> getTransactionsByUsername(@RequestParam(value = "username") String username);

    @PostMapping(value = "/api/transfer", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, String> transferRaw(@RequestBody Map<String, Object> request);
}
