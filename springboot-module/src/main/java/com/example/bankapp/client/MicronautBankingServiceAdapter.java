package com.example.bankapp.client;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.BankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("micronautBankingService")
public class MicronautBankingServiceAdapter implements BankingService {

    @Autowired
    private MicronautBankingClient client;

    @Override
    public Account findAccountByUsername(String username) {
        return client.findAccountByUsername(username);
    }

    @Override
    public Account registerAccount(String username, String password) {
        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("password", password);
        client.registerAccountRaw(request);
        return client.findAccountByUsername(username);
    }

    @Override
    public void deposit(Account account, BigDecimal amount) {
        Map<String, Object> request = new HashMap<>();
        request.put("amount", amount);
        request.put("username", account.getUsername());
        client.depositRaw(request);
    }

    @Override
    public void withdraw(Account account, BigDecimal amount) {
        Map<String, Object> request = new HashMap<>();
        request.put("amount", amount);
        request.put("username", account.getUsername());
        client.withdrawRaw(request);
    }

    @Override
    public List<Transaction> getTransactionHistory(Account account) {
        return client.getTransactionsByUsername(account.getUsername());
    }

    @Override
    public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
        Map<String, Object> request = new HashMap<>();
        request.put("toUsername", toUsername);
        request.put("amount", amount);
        request.put("fromUsername", fromAccount.getUsername());
        client.transferRaw(request);
    }
}
