package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

public interface BankingService {
    
    Account findAccountByUsername(String username);
    
    Account registerAccount(String username, String password);
    
    void deposit(Account account, BigDecimal amount);
    
    void withdraw(Account account, BigDecimal amount);
    
    List<Transaction> getTransactionHistory(Account account);
    
    void transferAmount(Account fromAccount, String toUsername, BigDecimal amount);
}
