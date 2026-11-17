package com.example.bankapp.repository;

import com.example.bankapp.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId")
    List<Transaction> find_by_account_id(@Param("accountId") Long accountId);
}
