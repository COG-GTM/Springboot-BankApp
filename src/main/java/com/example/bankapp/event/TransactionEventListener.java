package com.example.bankapp.event;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class TransactionEventListener {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Async
    @EventListener
    @Transactional
    public void handleTransactionEvent(TransactionEvent event) {
        Account account = accountRepository.findById(event.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Transaction transaction = new Transaction(
                event.getAmount(),
                event.getType(),
                LocalDateTime.now(),
                account
        );
        transactionRepository.save(transaction);
    }
}
