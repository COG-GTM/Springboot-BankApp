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
import java.time.LocalDateTime;
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

    public Account findAccountByUsername(String username) {
        return accountRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Account not found"));
    }

    @Transactional
    public Account registerAccount(String username, String password) {
        if (username == null || !username.matches("^[a-zA-Z0-9_]{3,50}$")) {
            throw new RuntimeException("Username must be 3-50 alphanumeric characters or underscores");
        }
        if (password == null || password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters");
        }
        if (accountRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Registration failed. Please try a different username.");
        }

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setBalance(BigDecimal.ZERO);
        return accountRepository.save(account);
    }


    @Transactional
    public void deposit(Account account, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }
        Account locked = accountRepository.findByUsernameForUpdate(account.getUsername())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        locked.setBalance(locked.getBalance().add(amount));
        accountRepository.save(locked);

        Transaction transaction = new Transaction(
                amount,
                "Deposit",
                LocalDateTime.now(),
                locked
        );
        transactionRepository.save(transaction);
    }

    @Transactional
    public void withdraw(Account account, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }
        Account locked = accountRepository.findByUsernameForUpdate(account.getUsername())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (locked.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        locked.setBalance(locked.getBalance().subtract(amount));
        accountRepository.save(locked);

        Transaction transaction = new Transaction(
                amount,
                "Withdrawal",
                LocalDateTime.now(),
                locked
        );
        transactionRepository.save(transaction);
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
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }
        Account lockedFrom = accountRepository.findByUsernameForUpdate(fromAccount.getUsername())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (lockedFrom.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        Account lockedTo = accountRepository.findByUsernameForUpdate(toUsername)
                .orElseThrow(() -> new RuntimeException("Recipient account not found"));

        lockedFrom.setBalance(lockedFrom.getBalance().subtract(amount));
        accountRepository.save(lockedFrom);

        lockedTo.setBalance(lockedTo.getBalance().add(amount));
        accountRepository.save(lockedTo);

        // Create transaction records for both accounts
        Transaction debitTransaction = new Transaction(
                amount,
                "Transfer Out to " + lockedTo.getUsername(),
                LocalDateTime.now(),
                lockedFrom
        );
        transactionRepository.save(debitTransaction);

        Transaction creditTransaction = new Transaction(
                amount,
                "Transfer In from " + lockedFrom.getUsername(),
                LocalDateTime.now(),
                lockedTo
        );
        transactionRepository.save(creditTransaction);
    }

}
