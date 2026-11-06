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
        Account locked = lockAccount(account.getId());
        locked.setBalance(locked.getBalance().add(amount));

        transactionRepository.save(new Transaction(amount, "Deposit", LocalDateTime.now(), locked));
        account.setBalance(locked.getBalance());
    }

    @Transactional
    public void withdraw(Account account, BigDecimal amount) {
        Account locked = lockAccount(account.getId());
        if (locked.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        locked.setBalance(locked.getBalance().subtract(amount));

        transactionRepository.save(new Transaction(amount, "Withdrawal", LocalDateTime.now(), locked));
        account.setBalance(locked.getBalance());
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
        Long toId = accountRepository.findByUsername(toUsername)
                .orElseThrow(() -> new RuntimeException("Recipient account not found"))
                .getId();

        // Lock both rows in ascending id order so opposite-direction transfers cannot deadlock.
        Long fromId = fromAccount.getId();
        Account first = lockAccount(Math.min(fromId, toId));
        Account second = lockAccount(Math.max(fromId, toId));
        Account from = fromId <= toId ? first : second;
        Account to = fromId <= toId ? second : first;

        if (from.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        transactionRepository.save(new Transaction(
                amount, "Transfer Out to " + to.getUsername(), LocalDateTime.now(), from));
        transactionRepository.save(new Transaction(
                amount, "Transfer In from " + from.getUsername(), LocalDateTime.now(), to));
        fromAccount.setBalance(from.getBalance());
    }

    // refresh() with a lock issues SELECT ... FOR UPDATE and overwrites any stale copy already in the
    // persistence context (open-in-view keeps the controller's Account managed for the whole request).
    private Account lockAccount(Long id) {
        Account account = entityManager.find(Account.class, id);
        if (account == null) {
            throw new RuntimeException("Account not found");
        }
        entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        return account;
    }

}
