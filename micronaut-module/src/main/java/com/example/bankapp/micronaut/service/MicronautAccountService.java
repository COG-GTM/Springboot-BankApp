package com.example.bankapp.micronaut.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.BankingService;
import jakarta.inject.Singleton;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class MicronautAccountService implements BankingService {

    private final Map<String, Account> accountStorage = new HashMap<>();
    private final Map<Long, List<Transaction>> transactionStorage = new HashMap<>();
    private final BCryptPasswordEncoder passwordEncoder;
    private Long nextAccountId = 1L;
    private Long nextTransactionId = 1L;

    public MicronautAccountService() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public Account findAccountByUsername(String username) {
        Account account = accountStorage.get(username);
        if (account == null) {
            throw new RuntimeException("Account not found");
        }
        return account;
    }

    @Override
    public Account registerAccount(String username, String password) {
        if (accountStorage.containsKey(username)) {
            throw new RuntimeException("Username already exists");
        }

        Account account = new Account();
        account.setId(nextAccountId++);
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setBalance(BigDecimal.ZERO);
        
        accountStorage.put(username, account);
        transactionStorage.put(account.getId(), new java.util.ArrayList<>());
        
        return account;
    }

    @Override
    public void deposit(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
        accountStorage.put(account.getUsername(), account);

        Transaction transaction = new Transaction(
                amount,
                "Deposit",
                LocalDateTime.now(),
                account
        );
        transaction.setId(nextTransactionId++);
        transactionStorage.get(account.getId()).add(transaction);
    }

    @Override
    public void withdraw(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountStorage.put(account.getUsername(), account);

        Transaction transaction = new Transaction(
                amount,
                "Withdrawal",
                LocalDateTime.now(),
                account
        );
        transaction.setId(nextTransactionId++);
        transactionStorage.get(account.getId()).add(transaction);
    }

    @Override
    public List<Transaction> getTransactionHistory(Account account) {
        return transactionStorage.getOrDefault(account.getId(), new java.util.ArrayList<>());
    }

    @Override
    public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        Account toAccount = accountStorage.get(toUsername);
        if (toAccount == null) {
            throw new RuntimeException("Recipient account not found");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountStorage.put(fromAccount.getUsername(), fromAccount);

        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountStorage.put(toAccount.getUsername(), toAccount);

        Transaction debitTransaction = new Transaction(
                amount,
                "Transfer Out to " + toAccount.getUsername(),
                LocalDateTime.now(),
                fromAccount
        );
        debitTransaction.setId(nextTransactionId++);
        transactionStorage.get(fromAccount.getId()).add(debitTransaction);

        Transaction creditTransaction = new Transaction(
                amount,
                "Transfer In from " + fromAccount.getUsername(),
                LocalDateTime.now(),
                toAccount
        );
        creditTransaction.setId(nextTransactionId++);
        transactionStorage.get(toAccount.getId()).add(creditTransaction);
    }
}
