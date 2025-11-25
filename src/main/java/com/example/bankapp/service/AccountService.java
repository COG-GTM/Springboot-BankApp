package com.example.bankapp.service;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.DuplicateUsernameException;
import com.example.bankapp.exception.InsufficientFundsException;
import com.example.bankapp.exception.InvalidAmountException;
import com.example.bankapp.exception.InvalidInputException;
import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.regex.Pattern;

@Service
public class AccountService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\w.-]+$");

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Account findAccountByUsername(String username) {
        logger.debug("Finding account by username: {}", username);
        validateUsername(username);
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Account not found for username: {}", username);
                    return new AccountNotFoundException("Account not found for username: " + username);
                });
    }

    @Transactional
    public Account registerAccount(String username, String password) {
        logger.info("Attempting to register new account for username: {}", username);
        
        validateUsername(username);
        validatePassword(password);
        
        if (accountRepository.findByUsername(username).isPresent()) {
            logger.error("Registration failed - username already exists: {}", username);
            throw new DuplicateUsernameException("Username already exists: " + username);
        }

        try {
            Account account = new Account();
            account.setUsername(username);
            account.setPassword(passwordEncoder.encode(password));
            account.setBalance(BigDecimal.ZERO);
            Account savedAccount = accountRepository.save(account);
            logger.info("Successfully registered new account for username: {}", username);
            return savedAccount;
        } catch (Exception e) {
            logger.error("Failed to register account for username: {} - Error: {}", username, e.getMessage(), e);
            throw new RuntimeException("Failed to register account: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deposit(Account account, BigDecimal amount) {
        logger.info("Processing deposit for account: {} - Amount: {}", account.getUsername(), amount);
        
        validateAmount(amount);
        
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
            logger.info("Deposit successful for account: {} - New balance: {}", 
                       account.getUsername(), account.getBalance());
        } catch (Exception e) {
            logger.error("Deposit failed for account: {} - Amount: {} - Error: {}", 
                        account.getUsername(), amount, e.getMessage(), e);
            throw new RuntimeException("Failed to process deposit: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void withdraw(Account account, BigDecimal amount) {
        logger.info("Processing withdrawal for account: {} - Amount: {}", account.getUsername(), amount);
        
        validateAmount(amount);
        
        if (account.getBalance().compareTo(amount) < 0) {
            logger.error("Withdrawal failed - insufficient funds for account: {} - Balance: {} - Requested: {}", 
                        account.getUsername(), account.getBalance(), amount);
            throw new InsufficientFundsException(
                    "Insufficient funds. Current balance: " + account.getBalance() + ", Requested: " + amount);
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
            logger.info("Withdrawal successful for account: {} - New balance: {}", 
                       account.getUsername(), account.getBalance());
        } catch (Exception e) {
            logger.error("Withdrawal failed for account: {} - Amount: {} - Error: {}", 
                        account.getUsername(), amount, e.getMessage(), e);
            throw new RuntimeException("Failed to process withdrawal: " + e.getMessage(), e);
        }
    }

    public List<Transaction> getTransactionHistory(Account account) {
        logger.debug("Retrieving transaction history for account: {}", account.getUsername());
        return transactionRepository.findByAccountId(account.getId());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user details for username: {}", username);
        
        try {
            Account account = findAccountByUsername(username);
            return new Account(
                    account.getUsername(),
                    account.getPassword(),
                    account.getBalance(),
                    account.getTransactions(),
                    authorities());
        } catch (AccountNotFoundException e) {
            logger.error("User not found during authentication: {}", username);
            throw new UsernameNotFoundException("Username or Password not found", e);
        }
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return Arrays.asList(new SimpleGrantedAuthority("USER"));
    }

    @Transactional
    public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
        logger.info("Processing transfer from: {} to: {} - Amount: {}", 
                   fromAccount.getUsername(), toUsername, amount);
        
        validateUsername(toUsername);
        validateAmount(amount);
        
        if (fromAccount.getUsername().equals(toUsername)) {
            logger.error("Transfer failed - cannot transfer to same account: {}", fromAccount.getUsername());
            throw new InvalidInputException("Cannot transfer to the same account");
        }
        
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            logger.error("Transfer failed - insufficient funds for account: {} - Balance: {} - Requested: {}", 
                        fromAccount.getUsername(), fromAccount.getBalance(), amount);
            throw new InsufficientFundsException(
                    "Insufficient funds. Current balance: " + fromAccount.getBalance() + ", Requested: " + amount);
        }

        Account toAccount = accountRepository.findByUsername(toUsername)
                .orElseThrow(() -> {
                    logger.error("Transfer failed - recipient account not found: {}", toUsername);
                    return new AccountNotFoundException("Recipient account not found: " + toUsername);
                });

        try {
            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            accountRepository.save(fromAccount);

            toAccount.setBalance(toAccount.getBalance().add(amount));
            accountRepository.save(toAccount);

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
            
            logger.info("Transfer successful from: {} to: {} - Amount: {}", 
                       fromAccount.getUsername(), toUsername, amount);
        } catch (Exception e) {
            logger.error("Transfer failed from: {} to: {} - Amount: {} - Error: {}", 
                        fromAccount.getUsername(), toUsername, amount, e.getMessage(), e);
            throw new RuntimeException("Failed to process transfer: " + e.getMessage(), e);
        }
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.error("Validation failed - username is null or empty");
            throw new InvalidInputException("Username cannot be null or empty");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            logger.error("Validation failed - invalid username format: {}", username);
            throw new InvalidInputException(
                    "Invalid username format. Username can only contain alphanumeric characters, underscores, dots, and hyphens");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            logger.error("Validation failed - password is null or empty");
            throw new InvalidInputException("Password cannot be null or empty");
        }
        if (password.length() < 6) {
            logger.error("Validation failed - password too short");
            throw new InvalidInputException("Password must be at least 6 characters long");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            logger.error("Validation failed - amount is null");
            throw new InvalidAmountException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Validation failed - amount must be positive: {}", amount);
            throw new InvalidAmountException("Amount must be greater than zero");
        }
    }
}
