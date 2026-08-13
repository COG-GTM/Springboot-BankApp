package com.example.bankapp.service;

import com.example.bankapp.audit.AuditLogger;
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

    public static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("1000000.00");

    private static final String ACTION_DEPOSIT = "DEPOSIT";
    private static final String ACTION_WITHDRAW = "WITHDRAW";
    private static final String ACTION_TRANSFER = "TRANSFER";

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AuditLogger auditLogger;

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

    /**
     * Rejects amounts that are null, non-positive, sub-cent or above the per-transaction limit.
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount must not have more than two decimal places");
        }
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount exceeds the per-transaction limit of " + MAX_TRANSACTION_AMOUNT);
        }
    }

    @Transactional
    public void deposit(Account account, BigDecimal amount) {
        try {
            validateAmount(amount);
        } catch (RuntimeException e) {
            auditLogger.failure(account.getUsername(), ACTION_DEPOSIT, amount, account.getId(), null, e.getMessage());
            throw e;
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                amount,
                "Deposit",
                LocalDateTime.now(),
                account
        );
        transactionRepository.save(transaction);

        auditLogger.success(account.getUsername(), ACTION_DEPOSIT, amount, account.getId(), null);
    }

    @Transactional
    public void withdraw(Account account, BigDecimal amount) {
        try {
            validateAmount(amount);
            if (account.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient funds");
            }
        } catch (RuntimeException e) {
            auditLogger.failure(account.getUsername(), ACTION_WITHDRAW, amount, account.getId(), null, e.getMessage());
            throw e;
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

        auditLogger.success(account.getUsername(), ACTION_WITHDRAW, amount, account.getId(), null);
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
        Account toAccount;
        try {
            validateAmount(amount);
            if (fromAccount.getUsername().equals(toUsername)) {
                throw new IllegalArgumentException("Cannot transfer to the same account");
            }
            toAccount = accountRepository.findByUsername(toUsername)
                    .orElseThrow(() -> new RuntimeException("Recipient account not found"));
            if (fromAccount.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient funds");
            }
        } catch (RuntimeException e) {
            auditLogger.failure(fromAccount.getUsername(), ACTION_TRANSFER, amount, fromAccount.getId(), null, e.getMessage());
            throw e;
        }

        // Deduct from sender's account
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepository.save(fromAccount);

        // Add to recipient's account
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        // Create transaction records for both accounts
        Transaction debitTransaction = new Transaction(
                amount,
                "Transfer Out to " + toAccount.getUsername(),
                LocalDateTime.now(),
                fromAccount
        );
        transactionRepository.save(debitTransaction);

        Transaction creditTransaction = new Transaction(
                amount,
                "Transfer In from " + fromAccount.getUsername(),
                LocalDateTime.now(),
                toAccount
        );
        transactionRepository.save(creditTransaction);

        auditLogger.success(fromAccount.getUsername(), ACTION_TRANSFER, amount, fromAccount.getId(), toAccount.getId());
    }

}
