package com.example.bankapp.service;

import com.example.bankapp.event.TransactionEvent;
import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.DuplicateUsernameException;
import com.example.bankapp.exception.InsufficientFundsException;
import com.example.bankapp.exception.InvalidAmountException;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public Account findAccountByUsername(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    @Transactional
    public Account registerAccount(String username, String password) {
        if (accountRepository.findByUsername(username).isPresent()) {
            throw new DuplicateUsernameException("Username already exists");
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
            throw new InvalidAmountException("Amount must be positive");
        }
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        eventPublisher.publishEvent(new TransactionEvent(
                amount, "Deposit", account.getId(), LocalDateTime.now()
        ));
    }

    @Transactional
    public void withdraw(Account account, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        eventPublisher.publishEvent(new TransactionEvent(
                amount, "Withdrawal", account.getId(), LocalDateTime.now()
        ));
    }

    public List<Transaction> getTransactionHistory(Account account) {
        return transactionRepository.findByAccountId(account.getId());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username or Password not found"));
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
            throw new InvalidAmountException("Amount must be positive");
        }
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        Account toAccount = accountRepository.findByUsername(toUsername)
                .orElseThrow(() -> new AccountNotFoundException("Recipient account not found"));

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepository.save(fromAccount);

        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        eventPublisher.publishEvent(new TransactionEvent(
                amount, "Transfer Out to " + toAccount.getUsername(),
                fromAccount.getId(), LocalDateTime.now()
        ));

        eventPublisher.publishEvent(new TransactionEvent(
                amount, "Transfer In from " + fromAccount.getUsername(),
                toAccount.getId(), LocalDateTime.now()
        ));
    }
}
