package com.example.bankapp.facade;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import com.example.bankapp.service.BankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class BankingServiceFacade implements BankingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BankingServiceFacade.class);
    
    @Autowired
    private AccountService springBootService;
    
    private BankingService micronautService;
    
    public Account findAccountByUsername(String username) {
        try {
            if (micronautService != null) {
                return micronautService.findAccountByUsername(username);
            }
        } catch (Exception e) {
            logger.error("Defaulting to Spring boot due to micronaut failure: " + e.getMessage(), e);
        }
        
        return springBootService.findAccountByUsername(username);
    }
    
    public Account registerAccount(String username, String password) {
        try {
            if (micronautService != null) {
                return micronautService.registerAccount(username, password);
            }
        } catch (Exception e) {
            logger.error("Defaulting to Spring boot due to micronaut failure: " + e.getMessage(), e);
        }
        
        return springBootService.registerAccount(username, password);
    }
    
    public void deposit(Account account, BigDecimal amount) {
        try {
            if (micronautService != null) {
                micronautService.deposit(account, amount);
                return;
            }
        } catch (Exception e) {
            logger.error("Defaulting to Spring boot due to micronaut failure: " + e.getMessage(), e);
        }
        
        springBootService.deposit(account, amount);
    }
    
    public void withdraw(Account account, BigDecimal amount) {
        try {
            if (micronautService != null) {
                micronautService.withdraw(account, amount);
                return;
            }
        } catch (Exception e) {
            logger.error("Defaulting to Spring boot due to micronaut failure: " + e.getMessage(), e);
        }
        
        springBootService.withdraw(account, amount);
    }
    
    public List<Transaction> getTransactionHistory(Account account) {
        try {
            if (micronautService != null) {
                return micronautService.getTransactionHistory(account);
            }
        } catch (Exception e) {
            logger.error("Defaulting to Spring boot due to micronaut failure: " + e.getMessage(), e);
        }
        
        return springBootService.getTransactionHistory(account);
    }
    
    public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
        try {
            if (micronautService != null) {
                micronautService.transferAmount(fromAccount, toUsername, amount);
                return;
            }
        } catch (Exception e) {
            logger.error("Defaulting to Spring boot due to micronaut failure: " + e.getMessage(), e);
        }
        
        springBootService.transferAmount(fromAccount, toUsername, amount);
    }
}
