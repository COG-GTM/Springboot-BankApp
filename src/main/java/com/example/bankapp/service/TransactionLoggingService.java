package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
public class TransactionLoggingService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionLoggingService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Async("paymentTaskExecutor")
    public CompletableFuture<Transaction> logDepositAsync(Account account, BigDecimal amount) {
        logger.info("Logging deposit asynchronously on thread: {}", Thread.currentThread().getName());
        Transaction transaction = new Transaction(amount, "Deposit", LocalDateTime.now(), account);
        transaction.setStatus(TransactionStatus.COMPLETED);
        Transaction saved = transactionRepository.save(transaction);
        return CompletableFuture.completedFuture(saved);
    }

    @Async("paymentTaskExecutor")
    public CompletableFuture<Transaction> logWithdrawalAsync(Account account, BigDecimal amount) {
        logger.info("Logging withdrawal asynchronously on thread: {}", Thread.currentThread().getName());
        Transaction transaction = new Transaction(amount, "Withdrawal", LocalDateTime.now(), account);
        transaction.setStatus(TransactionStatus.COMPLETED);
        Transaction saved = transactionRepository.save(transaction);
        return CompletableFuture.completedFuture(saved);
    }

    @Async("paymentTaskExecutor")
    public CompletableFuture<Transaction> logTransferAsync(Account account, BigDecimal amount, String type) {
        logger.info("Logging transfer asynchronously on thread: {}", Thread.currentThread().getName());
        Transaction transaction = new Transaction(amount, type, LocalDateTime.now(), account);
        transaction.setStatus(TransactionStatus.COMPLETED);
        Transaction saved = transactionRepository.save(transaction);
        return CompletableFuture.completedFuture(saved);
    }
}
