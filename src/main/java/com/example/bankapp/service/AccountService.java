package com.example.bankapp.service;

import com.example.bankapp.audit.AuditEvent;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class AccountService implements UserDetailsService {

    private static final String DEPOSIT = "DEPOSIT";
    private static final String WITHDRAWAL = "WITHDRAWAL";
    private static final String TRANSFER = "TRANSFER";

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


    public void deposit(Account account, BigDecimal amount) {
        try {
            account.setBalance(account.getBalance().add(amount));
            accountRepository.save(account);

            Transaction transaction = new Transaction(
                    amount,
                    "Deposit",
                    LocalDateTime.now(),
                    account
            );
            transactionRepository.save(transaction);
        } catch (RuntimeException e) {
            audit(DEPOSIT, AuditEvent.Outcome.FAILURE, account, null, amount, e.getMessage());
            throw e;
        }
        audit(DEPOSIT, AuditEvent.Outcome.SUCCESS, account, null, amount, null);
    }

    public void withdraw(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            audit(WITHDRAWAL, AuditEvent.Outcome.FAILURE, account, null, amount, "INSUFFICIENT_FUNDS");
            throw new RuntimeException("Insufficient funds");
        }
        try {
            account.setBalance(account.getBalance().subtract(amount));
            accountRepository.save(account);

            Transaction transaction = new Transaction(
                    amount,
                    "Withdrawal",
                    LocalDateTime.now(),
                    account
            );
            transactionRepository.save(transaction);
        } catch (RuntimeException e) {
            audit(WITHDRAWAL, AuditEvent.Outcome.FAILURE, account, null, amount, e.getMessage());
            throw e;
        }
        audit(WITHDRAWAL, AuditEvent.Outcome.SUCCESS, account, null, amount, null);
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

    public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            audit(TRANSFER, AuditEvent.Outcome.FAILURE, fromAccount, null, amount, "INSUFFICIENT_FUNDS");
            throw new RuntimeException("Insufficient funds");
        }

        Account toAccount = accountRepository.findByUsername(toUsername).orElse(null);
        if (toAccount == null) {
            audit(TRANSFER, AuditEvent.Outcome.FAILURE, fromAccount, null, amount, "RECIPIENT_NOT_FOUND");
            throw new RuntimeException("Recipient account not found");
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

        audit(TRANSFER, AuditEvent.Outcome.SUCCESS, fromAccount, toAccount, amount, null);
    }

    private void audit(String eventType, AuditEvent.Outcome outcome, Account account, Account counterparty,
                       BigDecimal amount, String reason) {
        auditLogger.log(AuditEvent.builder(eventType, outcome)
                .actor(account == null ? null : account.getUsername())
                .accountId(account == null ? null : account.getId())
                .counterpartyAccountId(counterparty == null ? null : counterparty.getId())
                .amount(amount)
                .reason(reason)
                .build());
    }

}
