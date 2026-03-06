package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class AccountService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(PasswordEncoder passwordEncoder,
                          AccountRepository accountRepository,
                          TransactionRepository transactionRepository) {
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Cacheable(value = "accounts", key = "#username")
    public Account findAccountByUsername(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#username")
    public Account registerAccount(String username, String password) {
        if (accountRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setBalance(BigDecimal.ZERO);
        return accountRepository.save(account);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#account.username")
    public void deposit(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                amount,
                "Deposit",
                LocalDateTime.now(),
                account
        );
        transactionRepository.save(transaction);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#account.username")
    public void withdraw(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                amount,
                "Withdrawal",
                LocalDateTime.now(),
                account
        );
        transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionHistory(Account account) {
        return transactionRepository.findByAccountId(account.getId());
    }

    public Page<Transaction> getTransactionHistory(Account account, Pageable pageable) {
        return transactionRepository.findByAccountId(account.getId(), pageable);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username or Password not found"));
        return new Account(
                account.getUsername(),
                account.getPassword(),
                account.getBalance(),
                null,
                authorities());
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return Arrays.asList(new SimpleGrantedAuthority("USER"));
    }

    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        Account toAccount = accountRepository.findByUsername(toUsername)
                .orElseThrow(() -> new RuntimeException("Recipient account not found"));

        // Deduct from sender's account
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        // Add to recipient's account
        toAccount.setBalance(toAccount.getBalance().add(amount));

        // Batch save both accounts in one flush
        accountRepository.saveAll(List.of(fromAccount, toAccount));

        // Create transaction records for both accounts
        Transaction debitTransaction = new Transaction(
                amount,
                "Transfer Out to " + toAccount.getUsername(),
                LocalDateTime.now(),
                fromAccount
        );
        Transaction creditTransaction = new Transaction(
                amount,
                "Transfer In from " + fromAccount.getUsername(),
                LocalDateTime.now(),
                toAccount
        );

        // Batch save both transactions in one flush
        transactionRepository.saveAll(List.of(debitTransaction, creditTransaction));
    }
}
