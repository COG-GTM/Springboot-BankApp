package com.example.bankapp.client;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.BankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

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
        client.registerAccountRaw(username, password);
        return client.findAccountByUsername(username);
    }

    @Override
    public void deposit(Account account, BigDecimal amount) {
        client.depositRaw(amount, account.getUsername());
    }

    @Override
    public void withdraw(Account account, BigDecimal amount) {
        client.withdrawRaw(amount, account.getUsername());
    }

    @Override
    public List<Transaction> getTransactionHistory(Account account) {
        return client.getTransactionsByUsername(account.getUsername());
    }

    @Override
    public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
        client.transferRaw(toUsername, amount, fromAccount.getUsername());
    }
}
