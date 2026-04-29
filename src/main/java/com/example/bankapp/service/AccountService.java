package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class AccountService implements UserDetailsService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionLoggingService transactionLoggingService;

    public Account findAccountByUsername(String username) {
        return accountRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public Account registerAccount(String username, String password) {
        if (accountRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password)); // Encrypt password
        account.setBalance(BigDecimal.ZERO); // Initial balance set to 0
        return accountRepository.save(account);
    }


    @Transactional
    public void deposit(Account account, BigDecimal amount) {
        Account freshAccount = accountRepository.findById(account.getId())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        freshAccount.setBalance(freshAccount.getBalance().add(amount));
        accountRepository.save(freshAccount);

        transactionLoggingService.logDepositAsync(freshAccount, amount);
    }

    @Transactional
    public void withdraw(Account account, BigDecimal amount) {
        Account freshAccount = accountRepository.findById(account.getId())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (freshAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        freshAccount.setBalance(freshAccount.getBalance().subtract(amount));
        accountRepository.save(freshAccount);

        transactionLoggingService.logWithdrawalAsync(freshAccount, amount);
    }

    public List<Transaction> getTransactionHistory(Account account) {
        return transactionRepository.findByAccountId(account.getId());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Account account = findAccountByUsername(username);
        if (account == null) {
            throw new UsernameNotFoundException("Username or Password not found");
        }
        return new Account(
                account.getUsername(),
                account.getPassword(),
                account.getBalance(),
                account.getTransactions(),
                authorities());
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return Arrays.asList(new SimpleGrantedAuthority("USER"));
    }

    @Transactional
    public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
        Account freshFrom = accountRepository.findById(fromAccount.getId())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (freshFrom.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        Account toAccount = accountRepository.findByUsername(toUsername)
                .orElseThrow(() -> new RuntimeException("Recipient account not found"));

        freshFrom.setBalance(freshFrom.getBalance().subtract(amount));
        accountRepository.save(freshFrom);

        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        transactionLoggingService.logTransferAsync(
                freshFrom, amount, "Transfer Out to " + toAccount.getUsername());
        transactionLoggingService.logTransferAsync(
                toAccount, amount, "Transfer In from " + freshFrom.getUsername());
    }

}
