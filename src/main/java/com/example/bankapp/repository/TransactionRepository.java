package com.example.bankapp.repository;

import com.example.bankapp.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountId(Long accountId);

    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);
}
