package com.example.bankapp.repository;

import com.example.bankapp.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByAccountIdAndTimestampAfterOrderByTimestampDesc(Long accountId, LocalDateTime after);
}
