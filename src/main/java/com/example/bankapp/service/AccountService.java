package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager entityManager;

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
        Account lockedAccount = lockAccount(account.getId());
        lockedAccount.setBalance(lockedAccount.getBalance().add(amount));
        accountRepository.save(lockedAccount);

        Transaction transaction = new Transaction(
                amount,
                "Deposit",
                LocalDateTime.now(),
                lockedAccount
        );
        transactionRepository.save(transaction);
    }

    @Transactional
    public void withdraw(Account account, BigDecimal amount) {
        Account lockedAccount = lockAccount(account.getId());
        if (lockedAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        lockedAccount.setBalance(lockedAccount.getBalance().subtract(amount));
        accountRepository.save(lockedAccount);

        Transaction transaction = new Transaction(
                amount,
                "Withdrawal",
                LocalDateTime.now(),
                lockedAccount
        );
        transactionRepository.save(transaction);
    }

    private Account lockAccount(Long id) {
        Account account = entityManager.find(Account.class, id);
        if (account == null) {
            throw new RuntimeException("Account not found");
        }
        // refresh under the row lock: an instance already managed by the (open-in-view)
        // persistence context may hold a balance read before the lock was taken
        entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        return account;
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
        Long recipientId = accountRepository.findByUsername(toUsername)
                .orElseThrow(() -> new RuntimeException("Recipient account not found"))
                .getId();

        // Lock both accounts in a consistent order so concurrent transfers cannot deadlock
        Account lockedFrom;
        Account lockedTo;
        if (fromAccount.getId() < recipientId) {
            lockedFrom = lockAccount(fromAccount.getId());
            lockedTo = lockAccount(recipientId);
        } else {
            lockedTo = lockAccount(recipientId);
            lockedFrom = lockAccount(fromAccount.getId());
        }

        if (lockedFrom.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // Deduct from sender's account
        lockedFrom.setBalance(lockedFrom.getBalance().subtract(amount));
        accountRepository.save(lockedFrom);

        // Add to recipient's account
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
