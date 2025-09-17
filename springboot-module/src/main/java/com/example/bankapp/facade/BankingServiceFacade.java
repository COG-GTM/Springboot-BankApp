package com.example.bankapp.facade;

import com.example.bankapp.client.MicronautBankingClient;
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
    
    @Autowired(required = false)
    private MicronautBankingClient micronautService;
    
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
        logger.info("BankingServiceFacade.registerAccount called for username: {}", username);
        logger.info("micronautService is null: {}", micronautService == null);
        
        try {
            if (micronautService != null) {
                logger.info("Attempting to call Micronaut service for registration");
                Account result = micronautService.registerAccount(username, password);
                logger.info("Micronaut service call successful for username: {}", username);
                return result;
            } else {
                logger.warn("micronautService is null, falling back to Spring Boot service");
            }
        } catch (Exception e) {
            logger.error("Defaulting to Spring boot due to micronaut failure: " + e.getMessage(), e);
        }
        
        logger.info("Using Spring Boot service for registration");
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
