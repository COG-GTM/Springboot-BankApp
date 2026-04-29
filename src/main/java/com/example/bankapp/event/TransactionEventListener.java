package com.example.bankapp.event;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TransactionEventListener {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionEventListener(TransactionRepository transactionRepository,
                                     AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransactionEvent(TransactionEvent event) {
        Account account = accountRepository.findById(event.getAccountId())
                .orElse(null);
        if (account == null) {
            return;
        }

        Transaction transaction = new Transaction(
                event.getAmount(),
                event.getType(),
                event.getTimestamp(),
                account
        );
        transactionRepository.save(transaction);
    }
}
