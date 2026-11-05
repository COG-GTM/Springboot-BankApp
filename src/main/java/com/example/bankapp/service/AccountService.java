package com.example.bankapp.service;

import com.example.bankapp.exception.InvalidTransferException;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.ProcessedTransfer;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.ProcessedTransferRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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

    /** Upper bound for a single transfer; anything larger must go through a manual process. */
    public static final BigDecimal MAX_TRANSFER_AMOUNT = new BigDecimal("1000000");

    private static final int MAX_TRANSFER_SCALE = 2;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ProcessedTransferRepository processedTransferRepository;

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
    public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount, String idempotencyKey) {
        validateAmount(amount);

        String recipient = toUsername == null ? "" : toUsername.trim();
        if (recipient.isEmpty()) {
            throw new InvalidTransferException("Recipient username is required");
        }
        if (recipient.equalsIgnoreCase(fromAccount.getUsername())) {
            throw new InvalidTransferException("Cannot transfer to your own account");
        }

        String key = idempotencyKey == null ? "" : idempotencyKey.trim();
        if (key.isEmpty()) {
            throw new InvalidTransferException("Missing transfer request identifier");
        }
        if (processedTransferRepository.existsByIdempotencyKey(key)) {
            throw new InvalidTransferException("Duplicate transfer request ignored");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        Account toAccount = accountRepository.findByUsername(recipient)
                .orElseThrow(() -> new RuntimeException("Recipient account not found"));

        recordIdempotencyKey(key, fromAccount.getUsername());

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
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidTransferException("Transfer amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be greater than zero");
        }
        if (amount.scale() > MAX_TRANSFER_SCALE) {
            throw new InvalidTransferException("Transfer amount cannot have more than two decimal places");
        }
        if (amount.compareTo(MAX_TRANSFER_AMOUNT) > 0) {
            throw new InvalidTransferException("Transfer amount exceeds the per-transaction limit");
        }
    }

    /**
     * Claims the idempotency key before any balance is touched so that two concurrent
     * submissions of the same request cannot both post; the unique constraint is the
     * authoritative guard.
     */
    private void recordIdempotencyKey(String key, String fromUsername) {
        try {
            processedTransferRepository.saveAndFlush(
                    new ProcessedTransfer(key, fromUsername, LocalDateTime.now()));
        } catch (DataIntegrityViolationException e) {
            throw new InvalidTransferException("Duplicate transfer request ignored");
        }
    }

}
