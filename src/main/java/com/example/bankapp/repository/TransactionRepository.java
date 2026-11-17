package com.example.bankapp.repository;

import com.example.bankapp.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByAccountIdAndTimestampBetween(
            Long accountId, LocalDateTime start, LocalDateTime end);

    List<Transaction> findByAccountIdAndTimestampBetweenOrderByTimestampAsc(
            Long accountId, LocalDateTime start, LocalDateTime end);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t " +
            "where t.account.id = :accountId " +
            "and t.timestamp between :start and :end " +
            "and t.type like 'Transfer Out%'")
    BigDecimal sumOutboundTransferAmountByAccountIdAndTimestampBetween(
            @Param("accountId") Long accountId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
